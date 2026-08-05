package com.skoryk.projects.meetler.availability.service;

import com.skoryk.projects.meetler.availability.dto.*;
import com.skoryk.projects.meetler.availability.model.*;
import com.skoryk.projects.meetler.availability.repository.*;
import com.skoryk.projects.meetler.availability.resolver.AvailabilityTemplateResolver;
import com.skoryk.projects.meetler.calendar.Calendar;
import com.skoryk.projects.meetler.calendar.CalendarRepository;
import com.skoryk.projects.meetler.subscription.SubscriptionLimitService;
import com.skoryk.projects.meetler.user.AppUser;
import com.skoryk.projects.meetler.user.AppUserRole;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AvailabilityTemplateService {

  private static final int AVAILABILITY_PAGE_SIZE = 50;
  private static final int MAX_RESOLUTION_DAYS = 366;

  private final AvailabilityTemplateRepository templateRepository;
  private final AvailabilityTemplateBlockRepository blockRepository;
  private final AvailabilityTemplateRecurringBlockRepository recurringBlockRepository;
  private final AvailabilityTemplateSourceCalendarRepository sourceCalendarRepository;
  private final CalendarRepository calendarRepository;
  private final AvailabilityTemplateResolver resolver;
  private final SubscriptionLimitService subscriptionLimitService;
  private final AvailabilityTemplateMapper availabilityTemplateMapper;

  @Transactional
  public AvailabilityTemplateResponse createTemplate(
      AppUser user, CreateAvailabilityTemplateRequest request) {
    validateGuestTemplateLimit(user);
    subscriptionLimitService.assertCanCreateAvailabilityTemplate(user);
    validateTimezone(request.getTimezone());
    validateDefaultAvailabilityStatus(request.getDefaultAvailabilityStatus());

    if (request.isDefault()) {
      templateRepository.clearDefaultForUser(user);
    }

    AvailabilityTemplate template =
        AvailabilityTemplate.builder()
            .user(user)
            .name(request.getName())
            .timezone(request.getTimezone())
            .isDefault(request.isDefault())
            .defaultAvailabilityStatus(
                defaultAvailabilityStatusOrFallback(request.getDefaultAvailabilityStatus()))
            .build();

    return availabilityTemplateMapper.toTemplateResponse(templateRepository.save(template));
  }

  public Page<AvailabilityTemplateResponse> getUserTemplates(AppUser user, int page) {
    return templateRepository
        .findByUserOrderByCreatedAtDesc(user, availabilityPage(page))
        .map(availabilityTemplateMapper::toTemplateResponse);
  }

  public AvailabilityTemplateResponse getTemplate(UUID templateId, AppUser user) {
    return availabilityTemplateMapper.toTemplateResponse(getOwnedTemplate(templateId, user));
  }

  @Transactional
  public AvailabilityTemplateResponse updateTemplate(
      UUID templateId, AppUser user, UpdateAvailabilityTemplateRequest request) {
    validateTimezone(request.getTimezone());
    validateDefaultAvailabilityStatus(request.getDefaultAvailabilityStatus());

    AvailabilityTemplate template = getOwnedTemplate(templateId, user);

    if (request.isDefault()) {
      templateRepository.clearDefaultForUser(user);
    }

    template.setName(request.getName());
    template.setTimezone(request.getTimezone());
    template.setDefault(request.isDefault());
    template.setDefaultAvailabilityStatus(
        defaultAvailabilityStatusOrFallback(request.getDefaultAvailabilityStatus()));

    return availabilityTemplateMapper.toTemplateResponse(templateRepository.save(template));
  }

  @Transactional
  public void deleteTemplate(UUID templateId, AppUser user) {
    templateRepository.delete(getOwnedTemplate(templateId, user));
  }

  @Transactional
  public AvailabilityBlockResponse addBlock(
      UUID templateId, AppUser user, AddAvailabilityBlockRequest request) {
    AvailabilityTemplate template = getOwnedTemplate(templateId, user);
    validateTimeRange(request.getStartsAt(), request.getEndsAt());

    AvailabilityBlockSource source =
        request.getSource() == null ? AvailabilityBlockSource.MANUAL : request.getSource();

    AvailabilityTemplateBlock block =
        AvailabilityTemplateBlock.builder()
            .template(template)
            .startsAt(request.getStartsAt())
            .endsAt(request.getEndsAt())
            .status(request.getStatus())
            .source(source)
            .note(request.getNote())
            .build();

    return availabilityTemplateMapper.toBlockResponse(blockRepository.save(block));
  }

  public Page<AvailabilityBlockResponse> getBlocks(
      UUID templateId, AppUser user, OffsetDateTime from, OffsetDateTime to, int page) {
    validateOffsetDateRange(from, to);
    AvailabilityTemplate template = getOwnedTemplate(templateId, user);
    Pageable pageable = availabilityPage(page);

    if (from == null && to == null) {
      return blockRepository
          .findByTemplateOrderByStartsAtAsc(template, pageable)
          .map(availabilityTemplateMapper::toBlockResponse);
    }
    if (from == null) {
      return blockRepository
          .findByTemplateStartingBefore(template, to, pageable)
          .map(availabilityTemplateMapper::toBlockResponse);
    }
    if (to == null) {
      return blockRepository
          .findByTemplateEndingAfter(template, from, pageable)
          .map(availabilityTemplateMapper::toBlockResponse);
    }

    return blockRepository
        .findByTemplateOverlappingRange(template, from, to, pageable)
        .map(availabilityTemplateMapper::toBlockResponse);
  }

  @Transactional
  public AvailabilityBlockResponse updateBlock(
      UUID templateId, UUID blockId, AppUser user, UpdateAvailabilityBlockRequest request) {
    AvailabilityTemplate template = getOwnedTemplate(templateId, user);
    AvailabilityTemplateBlock block =
        blockRepository
            .findByIdAndTemplate(blockId, template)
            .orElseThrow(() -> new IllegalArgumentException("Availability block not found"));

    validateTimeRange(request.getStartsAt(), request.getEndsAt());

    block.setStartsAt(request.getStartsAt());
    block.setEndsAt(request.getEndsAt());
    block.setStatus(request.getStatus());
    block.setNote(request.getNote());

    return availabilityTemplateMapper.toBlockResponse(blockRepository.save(block));
  }

  @Transactional
  public void deleteBlock(UUID templateId, UUID blockId, AppUser user) {
    AvailabilityTemplate template = getOwnedTemplate(templateId, user);
    AvailabilityTemplateBlock block =
        blockRepository
            .findByIdAndTemplate(blockId, template)
            .orElseThrow(() -> new IllegalArgumentException("Availability block not found"));

    blockRepository.delete(block);
  }

  @Transactional
  public RecurringAvailabilityBlockResponse addRecurringBlock(
      UUID templateId, AppUser user, AddRecurringAvailabilityBlockRequest request) {
    AvailabilityTemplate template = getOwnedTemplate(templateId, user);
    validateLocalTimeRange(request.getStartTime(), request.getEndTime());
    validateRecurringRule(request);

    AvailabilityTemplateRecurringBlock block =
        AvailabilityTemplateRecurringBlock.builder().template(template).build();
    applyRecurringRequest(block, request);

    return availabilityTemplateMapper.toRecurringBlockResponse(
        recurringBlockRepository.save(block));
  }

  @Transactional
  public RecurringAvailabilityBlockResponse updateRecurringBlock(
      UUID templateId, UUID blockId, AppUser user, AddRecurringAvailabilityBlockRequest request) {
    AvailabilityTemplate template = getOwnedTemplate(templateId, user);
    validateLocalTimeRange(request.getStartTime(), request.getEndTime());
    validateRecurringRule(request);

    AvailabilityTemplateRecurringBlock block =
        recurringBlockRepository
            .findByIdAndTemplate(blockId, template)
            .orElseThrow(
                () -> new IllegalArgumentException("Recurring availability block not found"));
    applyRecurringRequest(block, request);

    return availabilityTemplateMapper.toRecurringBlockResponse(
        recurringBlockRepository.save(block));
  }

  @Transactional
  public RecurringAvailabilityBlockResponse convertBlockToRecurring(
      UUID templateId,
      UUID blockId,
      AppUser user,
      ConvertAvailabilityBlockToRecurringRequest request) {
    AvailabilityTemplate template = getOwnedTemplate(templateId, user);
    AvailabilityTemplateBlock originalBlock =
        blockRepository
            .findByIdAndTemplate(blockId, template)
            .orElseThrow(() -> new IllegalArgumentException("Availability block not found"));

    AddRecurringAvailabilityBlockRequest recurringRequest =
        toRecurringRequest(originalBlock, request);
    validateLocalTimeRange(recurringRequest.getStartTime(), recurringRequest.getEndTime());
    validateRecurringRule(recurringRequest);

    AvailabilityTemplateRecurringBlock recurringBlock =
        AvailabilityTemplateRecurringBlock.builder().template(template).build();
    applyRecurringRequest(recurringBlock, recurringRequest);

    AvailabilityTemplateRecurringBlock savedBlock = recurringBlockRepository.save(recurringBlock);

    if (request.isDeleteOriginalBlock()) {
      blockRepository.delete(originalBlock);
    }

    return availabilityTemplateMapper.toRecurringBlockResponse(savedBlock);
  }

  public Page<RecurringAvailabilityBlockResponse> getRecurringBlocks(
      UUID templateId, AppUser user, LocalDate from, LocalDate to, int page) {
    validateLocalDateRange(from, to);
    AvailabilityTemplate template = getOwnedTemplate(templateId, user);
    Pageable pageable = availabilityPage(page);

    if (from == null && to == null) {
      return recurringBlockRepository
          .findByTemplateOrdered(template, pageable)
          .map(availabilityTemplateMapper::toRecurringBlockResponse);
    }
    if (from == null) {
      return recurringBlockRepository
          .findByTemplateStartingOnOrBefore(template, to, pageable)
          .map(availabilityTemplateMapper::toRecurringBlockResponse);
    }
    if (to == null) {
      return recurringBlockRepository
          .findByTemplateEndingOnOrAfter(template, from, pageable)
          .map(availabilityTemplateMapper::toRecurringBlockResponse);
    }

    return recurringBlockRepository
        .findByTemplateActiveInDateRange(template, from, to, pageable)
        .map(availabilityTemplateMapper::toRecurringBlockResponse);
  }

  public List<ResolvedAvailabilityWindowResponse> resolveTemplateAvailability(
      UUID templateId, AppUser user, OffsetDateTime from, OffsetDateTime to) {
    validateRequiredResolutionRange(from, to);

    AvailabilityTemplate template = getOwnedTemplate(templateId, user);

    return resolver.resolve(template, from, to);
  }

  @Transactional
  public void deleteRecurringBlock(UUID templateId, UUID blockId, AppUser user) {
    AvailabilityTemplate template = getOwnedTemplate(templateId, user);
    AvailabilityTemplateRecurringBlock block =
        recurringBlockRepository
            .findByIdAndTemplate(blockId, template)
            .orElseThrow(
                () -> new IllegalArgumentException("Recurring availability block not found"));

    recurringBlockRepository.delete(block);
  }

  @Transactional
  public SourceCalendarResponse addSourceCalendar(
      UUID templateId, AppUser user, AddSourceCalendarRequest request) {
    AvailabilityTemplate template = getOwnedTemplate(templateId, user);
    Calendar calendar =
        calendarRepository
            .findById(request.getCalendarId())
            .orElseThrow(() -> new IllegalArgumentException("Calendar not found"));

    if (!calendar.getUser().getId().equals(user.getId())) {
      throw new IllegalArgumentException("Calendar not found");
    }

    AvailabilityTemplateSourceCalendar source =
        sourceCalendarRepository
            .findByTemplateAndCalendar(template, calendar)
            .orElseGet(
                () ->
                    AvailabilityTemplateSourceCalendar.builder()
                        .template(template)
                        .calendar(calendar)
                        .build());

    source.setIncludeBusyEvents(request.isIncludeBusyEvents());

    return availabilityTemplateMapper.toSourceCalendarResponse(
        sourceCalendarRepository.save(source));
  }

  public List<SourceCalendarResponse> getSourceCalendars(UUID templateId, AppUser user) {
    AvailabilityTemplate template = getOwnedTemplate(templateId, user);
    return sourceCalendarRepository.findByTemplate(template).stream()
        .map(availabilityTemplateMapper::toSourceCalendarResponse)
        .toList();
  }

  @Transactional
  public void deleteSourceCalendar(UUID templateId, UUID sourceId, AppUser user) {
    AvailabilityTemplate template = getOwnedTemplate(templateId, user);
    AvailabilityTemplateSourceCalendar source =
        sourceCalendarRepository
            .findByIdAndTemplate(sourceId, template)
            .orElseThrow(() -> new IllegalArgumentException("Source calendar not found"));

    sourceCalendarRepository.delete(source);
  }

  private AvailabilityTemplate getOwnedTemplate(UUID templateId, AppUser user) {
    return templateRepository
        .findByIdAndUser(templateId, user)
        .orElseThrow(() -> new IllegalArgumentException("Availability template not found"));
  }

  private void validateGuestTemplateLimit(AppUser user) {
    if (user.getRole() == AppUserRole.GUEST && templateRepository.existsByUser(user)) {
      throw new IllegalStateException("Guest accounts can create only one availability template");
    }
  }

  private void validateTimezone(String timezone) {
    try {
      ZoneId.of(timezone);
    } catch (RuntimeException ex) {
      throw new IllegalArgumentException("Invalid timezone");
    }
  }

  private AvailabilityBlockStatus defaultAvailabilityStatusOrFallback(
      AvailabilityBlockStatus status) {
    return status == null ? AvailabilityBlockStatus.BUSY : status;
  }

  private void validateDefaultAvailabilityStatus(AvailabilityBlockStatus status) {
    defaultAvailabilityStatusOrFallback(status);
  }

  private Pageable availabilityPage(int page) {
    if (page < 0) {
      throw new IllegalArgumentException("Page must not be negative");
    }
    return PageRequest.of(page, AVAILABILITY_PAGE_SIZE);
  }

  private void validateOffsetDateRange(OffsetDateTime from, OffsetDateTime to) {
    if (from != null && to != null && !to.isAfter(from)) {
      throw new IllegalArgumentException("Range end must be after range start");
    }
  }

  private void validateLocalDateRange(LocalDate from, LocalDate to) {
    if (from != null && to != null && to.isBefore(from)) {
      throw new IllegalArgumentException("Range end date must not be before range start date");
    }
  }

  private void validateRequiredResolutionRange(OffsetDateTime from, OffsetDateTime to) {
    if (from == null || to == null) {
      throw new IllegalArgumentException("Resolution range requires from and to");
    }
    validateOffsetDateRange(from, to);
    if (Duration.between(from, to).toDays() > MAX_RESOLUTION_DAYS) {
      throw new IllegalArgumentException(
          "Resolution range must not be longer than " + MAX_RESOLUTION_DAYS + " days");
    }
  }

  private void validateTimeRange(OffsetDateTime startsAt, OffsetDateTime endsAt) {
    if (!endsAt.isAfter(startsAt)) {
      throw new IllegalArgumentException("Block end must be after start");
    }
  }

  private void validateLocalTimeRange(LocalTime startTime, LocalTime endTime) {
    if (!endTime.isAfter(startTime)) {
      throw new IllegalArgumentException("Recurring block end time must be after start time");
    }
  }

  private AddRecurringAvailabilityBlockRequest toRecurringRequest(
      AvailabilityTemplateBlock originalBlock, ConvertAvailabilityBlockToRecurringRequest request) {
    LocalDate originalDate = originalBlock.getStartsAt().toLocalDate();
    AddRecurringAvailabilityBlockRequest recurringRequest =
        new AddRecurringAvailabilityBlockRequest();
    recurringRequest.setFrequency(request.getFrequency());
    recurringRequest.setIntervalCount(request.getIntervalCount());
    recurringRequest.setOccurrenceCount(request.getOccurrenceCount());
    recurringRequest.setDayOfWeek(
        request.getDayOfWeek() == null ? originalDate.getDayOfWeek() : request.getDayOfWeek());
    recurringRequest.setDayOfMonth(
        request.getDayOfMonth() == null ? originalDate.getDayOfMonth() : request.getDayOfMonth());
    recurringRequest.setMonthOfYear(
        request.getMonthOfYear() == null ? originalDate.getMonthValue() : request.getMonthOfYear());
    recurringRequest.setStartTime(originalBlock.getStartsAt().toLocalTime());
    recurringRequest.setEndTime(originalBlock.getEndsAt().toLocalTime());
    recurringRequest.setStatus(originalBlock.getStatus());
    recurringRequest.setNote(originalBlock.getNote());
    recurringRequest.setStartsOn(
        request.getStartsOn() == null ? originalDate : request.getStartsOn());
    recurringRequest.setEndsOn(request.getEndsOn());

    clearUnusedRecurrenceFields(recurringRequest);
    return recurringRequest;
  }

  private void clearUnusedRecurrenceFields(AddRecurringAvailabilityBlockRequest request) {
    switch (request.getFrequency()) {
      case DAILY -> {
        request.setDayOfWeek(null);
        request.setDayOfMonth(null);
        request.setMonthOfYear(null);
      }
      case WEEKLY -> {
        request.setDayOfMonth(null);
        request.setMonthOfYear(null);
      }
      case MONTHLY -> {
        request.setDayOfWeek(null);
        request.setMonthOfYear(null);
      }
      case YEARLY -> request.setDayOfWeek(null);
      default -> throw new IllegalArgumentException("Unsupported recurrence frequency");
    }
  }

  private void applyRecurringRequest(
      AvailabilityTemplateRecurringBlock block, AddRecurringAvailabilityBlockRequest request) {
    block.setFrequency(request.getFrequency());
    block.setIntervalCount(request.getIntervalCount() == null ? 1 : request.getIntervalCount());
    block.setOccurrenceCount(request.getOccurrenceCount());
    block.setDayOfWeek(request.getDayOfWeek());
    block.setDayOfMonth(request.getDayOfMonth());
    block.setMonthOfYear(request.getMonthOfYear());
    block.setStartTime(request.getStartTime());
    block.setEndTime(request.getEndTime());
    block.setStatus(request.getStatus());
    block.setNote(request.getNote());
    block.setStartsOn(request.getStartsOn());
    block.setEndsOn(request.getEndsOn());
  }

  private void validateRecurringRule(AddRecurringAvailabilityBlockRequest request) {
    if (request.getIntervalCount() == null || request.getIntervalCount() < 1) {
      throw new IllegalArgumentException("Recurrence interval must be at least 1");
    }
    if (request.getOccurrenceCount() != null && request.getOccurrenceCount() < 1) {
      throw new IllegalArgumentException("Recurrence occurrence count must be at least 1");
    }

    if (request.getStartsOn() != null
        && request.getEndsOn() != null
        && request.getEndsOn().isBefore(request.getStartsOn())) {
      throw new IllegalArgumentException("Recurrence end date must not be before start date");
    }

    switch (request.getFrequency()) {
      case DAILY -> validateDailyRule(request);
      case WEEKLY -> validateWeeklyRule(request);
      case MONTHLY -> validateMonthlyRule(request);
      case YEARLY -> validateYearlyRule(request);
      default -> throw new IllegalArgumentException("Unsupported recurrence frequency");
    }
  }

  private void validateDailyRule(AddRecurringAvailabilityBlockRequest request) {
    if (request.getDayOfWeek() != null
        || request.getDayOfMonth() != null
        || request.getMonthOfYear() != null) {
      throw new IllegalArgumentException("Daily recurrence must not include day or month fields");
    }
  }

  private void validateWeeklyRule(AddRecurringAvailabilityBlockRequest request) {
    if (request.getDayOfWeek() == null) {
      throw new IllegalArgumentException("Weekly recurrence requires dayOfWeek");
    }
    if (request.getDayOfMonth() != null || request.getMonthOfYear() != null) {
      throw new IllegalArgumentException(
          "Weekly recurrence must not include dayOfMonth or monthOfYear");
    }
  }

  private void validateMonthlyRule(AddRecurringAvailabilityBlockRequest request) {
    if (request.getDayOfMonth() == null) {
      throw new IllegalArgumentException("Monthly recurrence requires dayOfMonth");
    }
    if (request.getDayOfWeek() != null || request.getMonthOfYear() != null) {
      throw new IllegalArgumentException(
          "Monthly recurrence must not include dayOfWeek or monthOfYear");
    }
  }

  private void validateYearlyRule(AddRecurringAvailabilityBlockRequest request) {
    if (request.getMonthOfYear() == null || request.getDayOfMonth() == null) {
      throw new IllegalArgumentException("Yearly recurrence requires monthOfYear and dayOfMonth");
    }
    if (request.getDayOfWeek() != null) {
      throw new IllegalArgumentException("Yearly recurrence must not include dayOfWeek");
    }

    try {
      MonthDay.of(request.getMonthOfYear(), request.getDayOfMonth());
    } catch (DateTimeException ex) {
      throw new IllegalArgumentException("Yearly recurrence date is invalid");
    }
  }
}
