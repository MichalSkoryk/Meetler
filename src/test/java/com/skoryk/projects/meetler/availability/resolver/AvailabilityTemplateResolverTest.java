package com.skoryk.projects.meetler.availability.resolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.skoryk.projects.meetler.availability.dto.ResolvedAvailabilityWindowResponse;
import com.skoryk.projects.meetler.availability.model.AvailabilityBlockSource;
import com.skoryk.projects.meetler.availability.model.AvailabilityBlockStatus;
import com.skoryk.projects.meetler.availability.model.AvailabilityRecurrenceFrequency;
import com.skoryk.projects.meetler.availability.model.AvailabilityTemplate;
import com.skoryk.projects.meetler.availability.model.AvailabilityTemplateBlock;
import com.skoryk.projects.meetler.availability.model.AvailabilityTemplateRecurringBlock;
import com.skoryk.projects.meetler.availability.model.AvailabilityTemplateSourceCalendar;
import com.skoryk.projects.meetler.availability.model.ResolvedAvailabilitySource;
import com.skoryk.projects.meetler.availability.repository.AvailabilityTemplateBlockRepository;
import com.skoryk.projects.meetler.availability.repository.AvailabilityTemplateRecurringBlockRepository;
import com.skoryk.projects.meetler.availability.repository.AvailabilityTemplateSourceCalendarRepository;
import com.skoryk.projects.meetler.calendar.Calendar;
import com.skoryk.projects.meetler.calendar.CalendarProvider;
import com.skoryk.projects.meetler.calendar.CalendarSynchronizationType;
import com.skoryk.projects.meetler.calendar.event.CalendarEvent;
import com.skoryk.projects.meetler.calendar.event.CalendarEventRepository;
import com.skoryk.projects.meetler.user.AppUser;
import com.skoryk.projects.meetler.user.AppUserRole;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AvailabilityTemplateResolverTest {

  @Mock private AvailabilityTemplateBlockRepository blockRepository;
  @Mock private AvailabilityTemplateRecurringBlockRepository recurringBlockRepository;
  @Mock private AvailabilityTemplateSourceCalendarRepository sourceCalendarRepository;
  @Mock private CalendarEventRepository calendarEventRepository;

  @InjectMocks private AvailabilityTemplateResolver resolver;

  private final OffsetDateTime from = OffsetDateTime.parse("2026-06-08T09:00:00+02:00");
  private final OffsetDateTime to = OffsetDateTime.parse("2026-06-08T12:00:00+02:00");

  @Test
  void resolveReturnsBusyDefaultWindowWhenTemplateHasNoBlocks() {
    AvailabilityTemplate template = template(AvailabilityBlockStatus.BUSY);
    stubEmptyRepositories(template);

    List<ResolvedAvailabilityWindowResponse> windows = resolver.resolve(template, from, to);

    assertThat(windows).hasSize(1);
    assertThat(windows.getFirst().getStartsAt()).isEqualTo(from);
    assertThat(windows.getFirst().getEndsAt()).isEqualTo(to);
    assertThat(windows.getFirst().getStatus()).isEqualTo(AvailabilityBlockStatus.BUSY);
    assertThat(windows.getFirst().getSource()).isEqualTo(ResolvedAvailabilitySource.DEFAULT);
  }

  @Test
  void resolveFillsOnlyGapsAroundRecurringAvailabilityWithDefaultBusy() {
    AvailabilityTemplate template = template(AvailabilityBlockStatus.BUSY);
    AvailabilityTemplateRecurringBlock recurring =
        recurringBlock(template, DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(11, 0));

    when(blockRepository.findByTemplateOverlappingRange(template, from, to)).thenReturn(List.of());
    when(sourceCalendarRepository.findByTemplate(template)).thenReturn(List.of());
    when(recurringBlockRepository.findByTemplateActiveInDateRange(
            template, LocalDate.parse("2026-06-08"), LocalDate.parse("2026-06-08")))
        .thenReturn(List.of(recurring));

    List<ResolvedAvailabilityWindowResponse> windows = resolver.resolve(template, from, to);

    assertThat(windows)
        .extracting(
            ResolvedAvailabilityWindowResponse::getStartsAt,
            ResolvedAvailabilityWindowResponse::getEndsAt,
            ResolvedAvailabilityWindowResponse::getStatus,
            ResolvedAvailabilityWindowResponse::getSource)
        .containsExactly(
            org.assertj.core.groups.Tuple.tuple(
                from,
                OffsetDateTime.parse("2026-06-08T10:00:00+02:00"),
                AvailabilityBlockStatus.BUSY,
                ResolvedAvailabilitySource.DEFAULT),
            org.assertj.core.groups.Tuple.tuple(
                OffsetDateTime.parse("2026-06-08T10:00:00+02:00"),
                OffsetDateTime.parse("2026-06-08T11:00:00+02:00"),
                AvailabilityBlockStatus.AVAILABLE,
                ResolvedAvailabilitySource.RECURRING),
            org.assertj.core.groups.Tuple.tuple(
                OffsetDateTime.parse("2026-06-08T11:00:00+02:00"),
                to,
                AvailabilityBlockStatus.BUSY,
                ResolvedAvailabilitySource.DEFAULT));
  }

  @Test
  void resolveOneOffBusyOverridesRecurringAvailability() {
    AvailabilityTemplate template = template(AvailabilityBlockStatus.BUSY);
    AvailabilityTemplateRecurringBlock recurring =
        recurringBlock(template, DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(12, 0));
    AvailabilityTemplateBlock oneOff =
        oneOffBlock(
            template,
            OffsetDateTime.parse("2026-06-08T10:00:00+02:00"),
            OffsetDateTime.parse("2026-06-08T11:00:00+02:00"),
            AvailabilityBlockStatus.BUSY);

    when(blockRepository.findByTemplateOverlappingRange(template, from, to))
        .thenReturn(List.of(oneOff));
    when(sourceCalendarRepository.findByTemplate(template)).thenReturn(List.of());
    when(recurringBlockRepository.findByTemplateActiveInDateRange(
            template, LocalDate.parse("2026-06-08"), LocalDate.parse("2026-06-08")))
        .thenReturn(List.of(recurring));

    List<ResolvedAvailabilityWindowResponse> windows = resolver.resolve(template, from, to);

    assertThat(windows)
        .extracting(
            ResolvedAvailabilityWindowResponse::getStartsAt,
            ResolvedAvailabilityWindowResponse::getEndsAt,
            ResolvedAvailabilityWindowResponse::getStatus,
            ResolvedAvailabilityWindowResponse::getSource)
        .containsExactly(
            org.assertj.core.groups.Tuple.tuple(
                from,
                OffsetDateTime.parse("2026-06-08T10:00:00+02:00"),
                AvailabilityBlockStatus.AVAILABLE,
                ResolvedAvailabilitySource.RECURRING),
            org.assertj.core.groups.Tuple.tuple(
                OffsetDateTime.parse("2026-06-08T10:00:00+02:00"),
                OffsetDateTime.parse("2026-06-08T11:00:00+02:00"),
                AvailabilityBlockStatus.BUSY,
                ResolvedAvailabilitySource.ONE_OFF),
            org.assertj.core.groups.Tuple.tuple(
                OffsetDateTime.parse("2026-06-08T11:00:00+02:00"),
                to,
                AvailabilityBlockStatus.AVAILABLE,
                ResolvedAvailabilitySource.RECURRING));
  }

  @Test
  void resolveIncludesExternalBusyEventsFromSourceCalendars() {
    AvailabilityTemplate template = template(AvailabilityBlockStatus.AVAILABLE);
    Calendar calendar = calendar(template.getUser());
    AvailabilityTemplateSourceCalendar source =
        AvailabilityTemplateSourceCalendar.builder()
            .id(UUID.randomUUID())
            .template(template)
            .calendar(calendar)
            .includeBusyEvents(true)
            .build();
    CalendarEvent event =
        CalendarEvent.builder()
            .id(UUID.randomUUID())
            .calendar(calendar)
            .externalId("event-1")
            .title("Doctor")
            .startsAt(OffsetDateTime.parse("2026-06-08T10:00:00+02:00"))
            .endsAt(OffsetDateTime.parse("2026-06-08T11:00:00+02:00"))
            .busy(true)
            .build();

    when(blockRepository.findByTemplateOverlappingRange(template, from, to)).thenReturn(List.of());
    when(recurringBlockRepository.findByTemplateActiveInDateRange(
            template, LocalDate.parse("2026-06-08"), LocalDate.parse("2026-06-08")))
        .thenReturn(List.of());
    when(sourceCalendarRepository.findByTemplate(template)).thenReturn(List.of(source));
    when(calendarEventRepository.findBusyEventsOverlapping(List.of(calendar), from, to))
        .thenReturn(List.of(event));

    List<ResolvedAvailabilityWindowResponse> windows = resolver.resolve(template, from, to);

    ResolvedAvailabilityWindowResponse importedWindow =
        windows.stream()
            .filter(window -> window.getSource() == ResolvedAvailabilitySource.EXTERNAL_CALENDAR)
            .findFirst()
            .orElseThrow();
    assertThat(importedWindow.getSourceCalendarId()).isEqualTo(calendar.getId());
    assertThat(importedWindow.getSourceCalendarName()).isEqualTo(calendar.getName());
    assertThat(importedWindow.getSourceCalendarProvider()).isEqualTo(calendar.getProvider());

    assertThat(windows)
        .extracting(
            ResolvedAvailabilityWindowResponse::getStartsAt,
            ResolvedAvailabilityWindowResponse::getEndsAt,
            ResolvedAvailabilityWindowResponse::getStatus,
            ResolvedAvailabilityWindowResponse::getSource)
        .containsExactly(
            org.assertj.core.groups.Tuple.tuple(
                from,
                OffsetDateTime.parse("2026-06-08T10:00:00+02:00"),
                AvailabilityBlockStatus.AVAILABLE,
                ResolvedAvailabilitySource.DEFAULT),
            org.assertj.core.groups.Tuple.tuple(
                OffsetDateTime.parse("2026-06-08T10:00:00+02:00"),
                OffsetDateTime.parse("2026-06-08T11:00:00+02:00"),
                AvailabilityBlockStatus.BUSY,
                ResolvedAvailabilitySource.EXTERNAL_CALENDAR),
            org.assertj.core.groups.Tuple.tuple(
                OffsetDateTime.parse("2026-06-08T11:00:00+02:00"),
                to,
                AvailabilityBlockStatus.AVAILABLE,
                ResolvedAvailabilitySource.DEFAULT));
  }

  private void stubEmptyRepositories(AvailabilityTemplate template) {
    when(blockRepository.findByTemplateOverlappingRange(template, from, to)).thenReturn(List.of());
    when(sourceCalendarRepository.findByTemplate(template)).thenReturn(List.of());
    when(recurringBlockRepository.findByTemplateActiveInDateRange(
            template, LocalDate.parse("2026-06-08"), LocalDate.parse("2026-06-08")))
        .thenReturn(List.of());
  }

  private AvailabilityTemplate template(AvailabilityBlockStatus defaultStatus) {
    AppUser user =
        AppUser.builder()
            .id(UUID.randomUUID())
            .email("test@example.com")
            .role(AppUserRole.USER)
            .build();
    return AvailabilityTemplate.builder()
        .id(UUID.randomUUID())
        .user(user)
        .name("Template")
        .timezone("Europe/Warsaw")
        .defaultAvailabilityStatus(defaultStatus)
        .build();
  }

  private AvailabilityTemplateRecurringBlock recurringBlock(
      AvailabilityTemplate template, DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime) {
    return AvailabilityTemplateRecurringBlock.builder()
        .id(UUID.randomUUID())
        .template(template)
        .frequency(AvailabilityRecurrenceFrequency.WEEKLY)
        .intervalCount(1)
        .dayOfWeek(dayOfWeek)
        .startTime(startTime)
        .endTime(endTime)
        .status(AvailabilityBlockStatus.AVAILABLE)
        .startsOn(LocalDate.parse("2026-06-01"))
        .build();
  }

  private AvailabilityTemplateBlock oneOffBlock(
      AvailabilityTemplate template,
      OffsetDateTime startsAt,
      OffsetDateTime endsAt,
      AvailabilityBlockStatus status) {
    return AvailabilityTemplateBlock.builder()
        .id(UUID.randomUUID())
        .template(template)
        .startsAt(startsAt)
        .endsAt(endsAt)
        .status(status)
        .source(AvailabilityBlockSource.MANUAL)
        .build();
  }

  private Calendar calendar(AppUser user) {
    return Calendar.builder()
        .id(UUID.randomUUID())
        .user(user)
        .name("Google")
        .provider(CalendarProvider.GOOGLE)
        .syncDirection(CalendarSynchronizationType.FROM_PROVIDER)
        .isActive(true)
        .isEditable(false)
        .build();
  }
}
