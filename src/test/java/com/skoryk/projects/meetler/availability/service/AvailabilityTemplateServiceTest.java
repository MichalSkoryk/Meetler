package com.skoryk.projects.meetler.availability.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.skoryk.projects.meetler.availability.dto.AddAvailabilityBlockRequest;
import com.skoryk.projects.meetler.availability.dto.AddRecurringAvailabilityBlockRequest;
import com.skoryk.projects.meetler.availability.dto.AvailabilityBlockResponse;
import com.skoryk.projects.meetler.availability.dto.AvailabilityTemplateResponse;
import com.skoryk.projects.meetler.availability.dto.CreateAvailabilityTemplateRequest;
import com.skoryk.projects.meetler.availability.model.AvailabilityBlockSource;
import com.skoryk.projects.meetler.availability.model.AvailabilityBlockStatus;
import com.skoryk.projects.meetler.availability.model.AvailabilityRecurrenceFrequency;
import com.skoryk.projects.meetler.availability.model.AvailabilityTemplate;
import com.skoryk.projects.meetler.availability.model.AvailabilityTemplateBlock;
import com.skoryk.projects.meetler.availability.model.AvailabilityTemplateRecurringBlock;
import com.skoryk.projects.meetler.availability.repository.AvailabilityTemplateBlockRepository;
import com.skoryk.projects.meetler.availability.repository.AvailabilityTemplateRecurringBlockRepository;
import com.skoryk.projects.meetler.availability.repository.AvailabilityTemplateRepository;
import com.skoryk.projects.meetler.availability.repository.AvailabilityTemplateSourceCalendarRepository;
import com.skoryk.projects.meetler.availability.resolver.AvailabilityTemplateResolver;
import com.skoryk.projects.meetler.calendar.CalendarRepository;
import com.skoryk.projects.meetler.subscription.SubscriptionLimitService;
import com.skoryk.projects.meetler.user.AppUser;
import com.skoryk.projects.meetler.user.AppUserRole;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class AvailabilityTemplateServiceTest {

  @Mock private AvailabilityTemplateRepository templateRepository;
  @Mock private AvailabilityTemplateBlockRepository blockRepository;
  @Mock private AvailabilityTemplateRecurringBlockRepository recurringBlockRepository;
  @Mock private AvailabilityTemplateSourceCalendarRepository sourceCalendarRepository;
  @Mock private CalendarRepository calendarRepository;
  @Mock private AvailabilityTemplateResolver resolver;
  @Mock private SubscriptionLimitService subscriptionLimitService;

  @Spy
  private AvailabilityTemplateMapper availabilityTemplateMapper =
      Mappers.getMapper(AvailabilityTemplateMapper.class);

  @InjectMocks private AvailabilityTemplateService service;

  @Test
  void createTemplateDefaultsEmptySpaceToBusy() {
    AppUser user = user();
    CreateAvailabilityTemplateRequest request = createTemplateRequest();
    request.setDefaultAvailabilityStatus(null);

    when(templateRepository.save(any(AvailabilityTemplate.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    AvailabilityTemplateResponse response = service.createTemplate(user, request);

    assertThat(response.getDefaultAvailabilityStatus()).isEqualTo(AvailabilityBlockStatus.BUSY);
    verify(templateRepository).clearDefaultForUser(user);
  }

  @Test
  void createTemplateAllowsAvailableEmptySpace() {
    AppUser user = user();
    CreateAvailabilityTemplateRequest request = createTemplateRequest();
    request.setDefaultAvailabilityStatus(AvailabilityBlockStatus.AVAILABLE);

    when(templateRepository.save(any(AvailabilityTemplate.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    AvailabilityTemplateResponse response = service.createTemplate(user, request);

    assertThat(response.getDefaultAvailabilityStatus())
        .isEqualTo(AvailabilityBlockStatus.AVAILABLE);
  }

  @Test
  void createTemplateRejectsInvalidTimezone() {
    CreateAvailabilityTemplateRequest request = createTemplateRequest();
    request.setTimezone("not/a-zone");

    assertThatThrownBy(() -> service.createTemplate(user(), request))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Invalid timezone");

    verify(templateRepository, never()).save(any());
  }

  @Test
  void createTemplateRejectsSecondGuestTemplate() {
    AppUser user = user();
    user.setRole(AppUserRole.GUEST);
    CreateAvailabilityTemplateRequest request = createTemplateRequest();
    when(templateRepository.existsByUser(user)).thenReturn(true);

    assertThatThrownBy(() -> service.createTemplate(user, request))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Guest accounts can create only one availability template");

    verify(templateRepository, never()).save(any());
  }

  @Test
  void addBlockDefaultsSourceToManual() {
    AppUser user = user();
    AvailabilityTemplate template = template(user);
    AddAvailabilityBlockRequest request = new AddAvailabilityBlockRequest();
    request.setStartsAt(OffsetDateTime.parse("2026-06-08T10:00:00+02:00"));
    request.setEndsAt(OffsetDateTime.parse("2026-06-08T11:00:00+02:00"));
    request.setStatus(AvailabilityBlockStatus.AVAILABLE);
    request.setSource(null);
    request.setNote("coffee");

    when(templateRepository.findByIdAndUser(template.getId(), user))
        .thenReturn(Optional.of(template));
    when(blockRepository.save(any(AvailabilityTemplateBlock.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    AvailabilityBlockResponse response = service.addBlock(template.getId(), user, request);

    assertThat(response.getSource()).isEqualTo(AvailabilityBlockSource.MANUAL);
    assertThat(response.getStatus()).isEqualTo(AvailabilityBlockStatus.AVAILABLE);
  }

  @Test
  void addBlockRejectsInvalidTimeRange() {
    AppUser user = user();
    AvailabilityTemplate template = template(user);
    AddAvailabilityBlockRequest request = new AddAvailabilityBlockRequest();
    request.setStartsAt(OffsetDateTime.parse("2026-06-08T11:00:00+02:00"));
    request.setEndsAt(OffsetDateTime.parse("2026-06-08T10:00:00+02:00"));
    request.setStatus(AvailabilityBlockStatus.BUSY);

    when(templateRepository.findByIdAndUser(template.getId(), user))
        .thenReturn(Optional.of(template));

    assertThatThrownBy(() -> service.addBlock(template.getId(), user, request))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Block end must be after start");

    verify(blockRepository, never()).save(any());
  }

  @Test
  void getBlocksUsesUnfilteredQueryWhenNoRangeProvided() {
    AppUser user = user();
    AvailabilityTemplate template = template(user);
    when(templateRepository.findByIdAndUser(template.getId(), user))
        .thenReturn(Optional.of(template));
    when(blockRepository.findByTemplateOrderByStartsAtAsc(eq(template), any()))
        .thenReturn(new PageImpl<>(java.util.List.of()));

    service.getBlocks(template.getId(), user, null, null, 0);

    ArgumentCaptor<PageRequest> pageable = ArgumentCaptor.forClass(PageRequest.class);
    verify(blockRepository).findByTemplateOrderByStartsAtAsc(eq(template), pageable.capture());
    assertThat(pageable.getValue().getPageSize()).isEqualTo(50);
  }

  @Test
  void getBlocksRejectsNegativePage() {
    AppUser user = user();
    AvailabilityTemplate template = template(user);
    when(templateRepository.findByIdAndUser(template.getId(), user))
        .thenReturn(Optional.of(template));

    assertThatThrownBy(() -> service.getBlocks(template.getId(), user, null, null, -1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Page must not be negative");
  }

  @Test
  void addRecurringBlockRejectsWeeklyRuleWithoutDayOfWeek() {
    AppUser user = user();
    AvailabilityTemplate template = template(user);
    AddRecurringAvailabilityBlockRequest request = recurringRequest();
    request.setFrequency(AvailabilityRecurrenceFrequency.WEEKLY);
    request.setDayOfWeek(null);

    when(templateRepository.findByIdAndUser(template.getId(), user))
        .thenReturn(Optional.of(template));

    assertThatThrownBy(() -> service.addRecurringBlock(template.getId(), user, request))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Weekly recurrence requires dayOfWeek");
  }

  @Test
  void updateRecurringBlockChangesTheExistingRule() {
    AppUser user = user();
    AvailabilityTemplate template = template(user);
    UUID blockId = UUID.randomUUID();
    AvailabilityTemplateRecurringBlock block =
        AvailabilityTemplateRecurringBlock.builder()
            .id(blockId)
            .template(template)
            .frequency(AvailabilityRecurrenceFrequency.WEEKLY)
            .intervalCount(1)
            .dayOfWeek(DayOfWeek.MONDAY)
            .startTime(LocalTime.of(9, 0))
            .endTime(LocalTime.of(17, 0))
            .status(AvailabilityBlockStatus.AVAILABLE)
            .build();
    AddRecurringAvailabilityBlockRequest request = recurringRequest();
    request.setDayOfWeek(DayOfWeek.WEDNESDAY);
    request.setIntervalCount(2);
    request.setStartTime(LocalTime.of(18, 0));
    request.setEndTime(LocalTime.of(20, 0));
    request.setStatus(AvailabilityBlockStatus.BUSY);
    request.setNote("Training");

    when(templateRepository.findByIdAndUser(template.getId(), user))
        .thenReturn(Optional.of(template));
    when(recurringBlockRepository.findByIdAndTemplate(blockId, template))
        .thenReturn(Optional.of(block));
    when(recurringBlockRepository.save(block)).thenReturn(block);

    service.updateRecurringBlock(template.getId(), blockId, user, request);

    assertThat(block.getDayOfWeek()).isEqualTo(DayOfWeek.WEDNESDAY);
    assertThat(block.getIntervalCount()).isEqualTo(2);
    assertThat(block.getStartTime()).isEqualTo(LocalTime.of(18, 0));
    assertThat(block.getEndTime()).isEqualTo(LocalTime.of(20, 0));
    assertThat(block.getStatus()).isEqualTo(AvailabilityBlockStatus.BUSY);
    assertThat(block.getNote()).isEqualTo("Training");
    verify(recurringBlockRepository).save(block);
  }

  private CreateAvailabilityTemplateRequest createTemplateRequest() {
    CreateAvailabilityTemplateRequest request = new CreateAvailabilityTemplateRequest();
    request.setName("Work hours");
    request.setDefault(true);
    request.setTimezone("Europe/Warsaw");
    return request;
  }

  private AddRecurringAvailabilityBlockRequest recurringRequest() {
    AddRecurringAvailabilityBlockRequest request = new AddRecurringAvailabilityBlockRequest();
    request.setFrequency(AvailabilityRecurrenceFrequency.WEEKLY);
    request.setIntervalCount(1);
    request.setDayOfWeek(DayOfWeek.MONDAY);
    request.setStartTime(LocalTime.of(9, 0));
    request.setEndTime(LocalTime.of(17, 0));
    request.setStatus(AvailabilityBlockStatus.AVAILABLE);
    request.setStartsOn(LocalDate.parse("2026-06-01"));
    return request;
  }

  private AvailabilityTemplate template(AppUser user) {
    return AvailabilityTemplate.builder()
        .id(UUID.randomUUID())
        .user(user)
        .name("Work hours")
        .timezone("Europe/Warsaw")
        .isDefault(true)
        .defaultAvailabilityStatus(AvailabilityBlockStatus.BUSY)
        .build();
  }

  private AppUser user() {
    return AppUser.builder()
        .id(UUID.randomUUID())
        .email("test@example.com")
        .name("Test User")
        .role(AppUserRole.USER)
        .build();
  }
}
