package com.skoryk.projects.meetler.availability.resolver;

import com.skoryk.projects.meetler.availability.dto.ResolvedAvailabilityWindowResponse;
import com.skoryk.projects.meetler.availability.model.AvailabilityTemplate;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.skoryk.projects.meetler.availability.model.AvailabilityTemplateSourceCalendar;
import com.skoryk.projects.meetler.availability.model.AvailabilityTemplateBlock;
import com.skoryk.projects.meetler.availability.model.AvailabilityTemplateRecurringBlock;
import com.skoryk.projects.meetler.availability.model.ResolvedAvailabilitySource;
import com.skoryk.projects.meetler.availability.repository.AvailabilityTemplateBlockRepository;
import com.skoryk.projects.meetler.availability.repository.AvailabilityTemplateRecurringBlockRepository;
import com.skoryk.projects.meetler.availability.repository.AvailabilityTemplateSourceCalendarRepository;
import com.skoryk.projects.meetler.calendar.Calendar;
import com.skoryk.projects.meetler.calendar.event.CalendarEvent;
import com.skoryk.projects.meetler.calendar.event.CalendarEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;



@Component
@RequiredArgsConstructor
public class AvailabilityTemplateResolver {

  private final AvailabilityTemplateBlockRepository blockRepository;
  private final AvailabilityTemplateRecurringBlockRepository recurringBlockRepository;
  private final AvailabilityTemplateSourceCalendarRepository sourceCalendarRepository;
  private final CalendarEventRepository calendarEventRepository;

  public List<ResolvedAvailabilityWindowResponse> resolve(
      AvailabilityTemplate template, OffsetDateTime from, OffsetDateTime to) {

    ZoneId zoneId = ZoneId.of(template.getTimezone());
    LocalDate fromDate = from.atZoneSameInstant(zoneId).toLocalDate();
    LocalDate toDate = to.atZoneSameInstant(zoneId).toLocalDate();

    List<AvailabilityTemplateBlock> oneOffBlocks = blockRepository.findByTemplateOverlappingRange(template, from, to);

    List<ResolvedAvailabilityWindowResponse> oneOffWindows = oneOffBlocks.stream().map(block -> toResolvedOneOffWindow(block, from, to)).toList();
    List<ResolvedAvailabilityWindowResponse> externalBusyWindows = resolveExternalBusyWindows(template, from, to);

    List<ResolvedAvailabilityWindowResponse> recurringWindows = recurringBlockRepository.findByTemplateActiveInDateRange(template, fromDate, toDate)
            .stream().flatMap(block -> expandRecurringBlock(block, zoneId, from, to, fromDate, toDate).stream()).toList();

    List<ResolvedAvailabilityWindowResponse> overrideWindows = new ArrayList<>();
    overrideWindows.addAll(oneOffWindows);
    overrideWindows.addAll(externalBusyWindows);

    List<ResolvedAvailabilityWindowResponse> resolved = new ArrayList<>();
    recurringWindows.stream()
            .flatMap(window -> subtractOneOffOverlaps(window, overrideWindows).stream())
            .forEach(resolved::add);

    resolved.addAll(oneOffWindows);
    resolved.addAll(externalBusyWindows);

    resolved.sort(
            Comparator.comparing(ResolvedAvailabilityWindowResponse::getStartsAt)
                    .thenComparing(ResolvedAvailabilityWindowResponse::getEndsAt)
                    .thenComparing(window -> window.getSource().name()));

    return resolved;
    }

  private List<ResolvedAvailabilityWindowResponse> resolveExternalBusyWindows(
      AvailabilityTemplate template, OffsetDateTime from, OffsetDateTime to) {
    List<Calendar> sourceCalendars =
        sourceCalendarRepository.findByTemplate(template).stream()
            .filter(AvailabilityTemplateSourceCalendar::isIncludeBusyEvents)
            .map(AvailabilityTemplateSourceCalendar::getCalendar)
            .toList();

    if (sourceCalendars.isEmpty()) {
      return List.of();
    }

    return calendarEventRepository.findBusyEventsOverlapping(sourceCalendars, from, to).stream()
        .map(event -> toResolvedExternalWindow(event, from, to))
        .toList();
  }

  private ResolvedAvailabilityWindowResponse toResolvedExternalWindow(
      CalendarEvent event, OffsetDateTime from, OffsetDateTime to) {
    return ResolvedAvailabilityWindowResponse.builder()
        .startsAt(max(event.getStartsAt(), from))
        .endsAt(min(event.getEndsAt(), to))
        .status(com.skoryk.projects.meetler.availability.model.AvailabilityBlockStatus.BUSY)
        .source(ResolvedAvailabilitySource.EXTERNAL_CALENDAR)
        .sourceBlockId(event.getId())
        .note(event.getTitle())
        .build();
  }

  private List<ResolvedAvailabilityWindowResponse> subtractOneOffOverlaps(ResolvedAvailabilityWindowResponse recurringWindow, List<ResolvedAvailabilityWindowResponse> oneOffWindows) {
      List<OffsetRange> segments =
              new ArrayList<>(List.of(new OffsetRange(recurringWindow.getStartsAt(), recurringWindow.getEndsAt())));

      for (ResolvedAvailabilityWindowResponse oneOffWindow : oneOffWindows) {
        List<OffsetRange> nextSegments = new ArrayList<>();
        for (OffsetRange segment : segments) {
          if (!overlaps(segment.startsAt(), segment.endsAt(), oneOffWindow.getStartsAt(), oneOffWindow.getEndsAt())) {
            nextSegments.add(segment);
            continue;
          }
          if (oneOffWindow.getStartsAt().isAfter(segment.startsAt())) {
            nextSegments.add(new OffsetRange(segment.startsAt(), oneOffWindow.getStartsAt()));
          }
          if (oneOffWindow.getEndsAt().isBefore(segment.endsAt())) {
            nextSegments.add(new OffsetRange(oneOffWindow.getEndsAt(), segment.endsAt()));
          }
        }
        segments = nextSegments;
        if (segments.isEmpty()) {
          break;
        }
      }

      return segments.stream()
              .filter(segment -> segment.endsAt().isAfter(segment.startsAt()))
              .map(
                      segment ->
                              ResolvedAvailabilityWindowResponse.builder()
                                      .startsAt(segment.startsAt())
                                      .endsAt(segment.endsAt())
                                      .status(recurringWindow.getStatus())
                                      .source(recurringWindow.getSource())
                                      .sourceBlockId(recurringWindow.getSourceBlockId())
                                      .note(recurringWindow.getNote())
                                      .build())
              .toList();
    }

  private boolean overlaps(OffsetDateTime firstStart, OffsetDateTime firstEnd, OffsetDateTime secondStart, OffsetDateTime secondEnd) {
    return firstStart.isBefore(secondEnd) && firstEnd.isAfter(secondStart);
  }

  private List<ResolvedAvailabilityWindowResponse> expandRecurringBlock(AvailabilityTemplateRecurringBlock block, ZoneId zoneId, OffsetDateTime from, OffsetDateTime to, LocalDate fromDate, LocalDate toDate) {
    return switch (block.getFrequency()) {
      case DAILY -> expandDailyRecurringBlock(block, zoneId, from, to, fromDate, toDate);
      case WEEKLY -> expandWeeklyRecurringBlock(block, zoneId, from, to, fromDate, toDate);
      case MONTHLY -> expandMonthlyRecurringBlock(block, zoneId, from, to, fromDate, toDate);
      case YEARLY -> expandYearlyRecurringBlock(block, zoneId, from, to, fromDate, toDate);
    };
  }

    private List<ResolvedAvailabilityWindowResponse> expandYearlyRecurringBlock(AvailabilityTemplateRecurringBlock block, ZoneId zoneId, OffsetDateTime from, OffsetDateTime to, LocalDate fromDate, LocalDate toDate) {
      LocalDate anchorDate = block.getStartsOn() == null ? fromDate : block.getStartsOn();
      int effectiveEndYear = effectiveEndDate(block, toDate).getYear();
      int interval = block.getIntervalCount();

      List<ResolvedAvailabilityWindowResponse> windows = new ArrayList<>();
      long occurrenceIndex = 0;
      for (int year = anchorDate.getYear(); year <= effectiveEndYear; year += interval) {
        LocalDate date = dateInYear(year, block.getMonthOfYear(), block.getDayOfMonth());
        if (date == null || date.isBefore(anchorDate)) {
          continue;
        }
        if (!withinOccurrenceCount(block, occurrenceIndex)) {
          break;
        }
        if (!date.isBefore(fromDate)) {
          addRecurringOccurrence(windows, block, zoneId, date, from, to);
        }
        occurrenceIndex++;
      }
      return windows;
    }

  private List<ResolvedAvailabilityWindowResponse> expandMonthlyRecurringBlock(AvailabilityTemplateRecurringBlock block, ZoneId zoneId, OffsetDateTime from, OffsetDateTime to, LocalDate fromDate, LocalDate toDate) {
        LocalDate anchorDate = block.getStartsOn() == null ? fromDate : block.getStartsOn();
        YearMonth anchorMonth = YearMonth.from(anchorDate);
        YearMonth effectiveEndMonth = YearMonth.from(effectiveEndDate(block, toDate));
        int interval = block.getIntervalCount();

        List<ResolvedAvailabilityWindowResponse> windows = new ArrayList<>();
        long occurrenceIndex = 0;
        for(YearMonth month = anchorMonth;!month.isAfter(effectiveEndMonth);month = month.plusMonths(interval)){
          LocalDate date = dateInMonth(month, block.getDayOfMonth());
          if(date == null || date.isBefore(anchorDate)) {
            continue;
          }
          if(!withinOccurrenceCount(block, occurrenceIndex)){
            break;
          }
          if(!date.isBefore(fromDate)){
            addRecurringOccurrence(windows, block, zoneId, date, from, to);
          }
          occurrenceIndex++;
        }
        return windows;
    }

  private LocalDate dateInYear(int year, int monthOfYear, int dayOfMonth){
    try{
      return LocalDate.of(year, monthOfYear, dayOfMonth);
    } catch (DateTimeException ex){
      return null;
    }
  }

  private LocalDate dateInMonth(YearMonth month, Integer dayOfMonth) {
    if(dayOfMonth > month.lengthOfMonth()) {
      return null;
    }
    return month.atDay(dayOfMonth);
  }

  private List<ResolvedAvailabilityWindowResponse> expandWeeklyRecurringBlock(AvailabilityTemplateRecurringBlock block, ZoneId zoneId, OffsetDateTime from, OffsetDateTime to, LocalDate fromDate, LocalDate toDate) {
        LocalDate anchorStart = block.getStartsOn() == null ? fromDate : block.getStartsOn();
        LocalDate anchorDate = firstOnOrAfter(anchorStart, block.getDayOfWeek());
        LocalDate effectiveEndDate = effectiveEndDate(block, toDate);
        int interval = block.getIntervalCount();
        long firstIndex = Math.max(0, ceilingDiv(ChronoUnit.WEEKS.between(anchorDate, fromDate), interval));

    List<ResolvedAvailabilityWindowResponse> windows = new ArrayList<>();
    for (long index = firstIndex; withinOccurrenceCount(block, index); index++){
      LocalDate date = anchorDate.plusWeeks(index * interval);
      if(date.isAfter(effectiveEndDate))
      {
        break;
      }
      addRecurringOccurrence(windows, block, zoneId, date, from, to);
    }

    return windows;
  }

  private LocalDate firstOnOrAfter(LocalDate date, DayOfWeek dayOfWeek) {
    int daysToAdd = (dayOfWeek.getValue() - date.getDayOfWeek().getValue() + 7) % 7;
    return date.plusDays(daysToAdd);
  }

  private List<ResolvedAvailabilityWindowResponse> expandDailyRecurringBlock(AvailabilityTemplateRecurringBlock block, ZoneId zoneId, OffsetDateTime from, OffsetDateTime to, LocalDate fromDate, LocalDate toDate) {
        LocalDate anchorDate = block.getStartsOn() == null ? fromDate : block.getStartsOn();
        LocalDate effectiveEndDate = effectiveEndDate(block, toDate);
        int interval = block.getIntervalCount();
        long firstIndex = Math.max(0, ceilingDiv(ChronoUnit.DAYS.between(anchorDate, fromDate), interval));

        List<ResolvedAvailabilityWindowResponse> windows = new ArrayList<>();
        for (long index = firstIndex; withinOccurrenceCount(block, index); index++){
          LocalDate date = anchorDate.plusDays(index * interval);
          if(date.isAfter(effectiveEndDate))
          {
            break;
          }
          addRecurringOccurrence(windows, block, zoneId, date, from, to);
        }

        return windows;
    }

  private void addRecurringOccurrence(List<ResolvedAvailabilityWindowResponse> windows, AvailabilityTemplateRecurringBlock block, ZoneId zoneId, LocalDate date, OffsetDateTime from, OffsetDateTime to) {
    OffsetDateTime startsAt = toOffsetDateTime(date, block.getStartTime(), zoneId);
    OffsetDateTime endsAt = toOffsetDateTime(date, block.getEndTime(), zoneId);

    if(!endsAt.isAfter(from) || !startsAt.isBefore(to)){
      return;
    }
    windows.add(
            ResolvedAvailabilityWindowResponse.builder()
                    .startsAt(max(startsAt, from))
                    .endsAt(min(endsAt, to))
                    .status(block.getStatus())
                    .source(ResolvedAvailabilitySource.RECURRING)
                    .sourceBlockId(block.getId())
                    .note(block.getNote())
                    .build()
    );
  }

  private OffsetDateTime toOffsetDateTime(LocalDate date, LocalTime time, ZoneId zoneId){
    return ZonedDateTime.of(date, time, zoneId).toOffsetDateTime();
  }

  private boolean withinOccurrenceCount(AvailabilityTemplateRecurringBlock block, long index) {
    return block.getOccurrenceCount() == null || index < block.getOccurrenceCount();
  }

  private long ceilingDiv(long value, int divisor) {
    if (value <= 0)
      return 0;
    return (value + divisor - 1) / divisor;
  }

  private LocalDate effectiveEndDate(AvailabilityTemplateRecurringBlock block, LocalDate toDate) {
    if(block.getEndsOn() == null || block.getEndsOn().isAfter(toDate)){
      return toDate;
    }
    return block.getEndsOn();
  }


  private ResolvedAvailabilityWindowResponse toResolvedOneOffWindow(AvailabilityTemplateBlock block, OffsetDateTime from, OffsetDateTime to) {
    return ResolvedAvailabilityWindowResponse.builder()
            .startsAt(max(block.getStartsAt(), from))
            .endsAt(min(block.getEndsAt(), to))
            .status(block.getStatus())
            .source(ResolvedAvailabilitySource.ONE_OFF)
            .sourceBlockId(block.getId())
            .note(block.getNote())
            .build();
  }


  private OffsetDateTime min(OffsetDateTime first, OffsetDateTime second){
    return first.isBefore(second) ? first : second;
  }

  private OffsetDateTime max(OffsetDateTime first, OffsetDateTime second){
    return first.isAfter(second) ? first : second;
  }

  private record OffsetRange(OffsetDateTime startsAt, OffsetDateTime endsAt){}



}
