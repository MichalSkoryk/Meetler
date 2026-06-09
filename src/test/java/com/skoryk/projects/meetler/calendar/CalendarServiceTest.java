package com.skoryk.projects.meetler.calendar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.skoryk.projects.meetler.calendar.dto.CalendarResponse;
import com.skoryk.projects.meetler.calendar.dto.CreateCalendarRequest;
import com.skoryk.projects.meetler.calendar.dto.UpdateCalendarRequest;
import com.skoryk.projects.meetler.user.AppUser;
import com.skoryk.projects.meetler.user.AppUserRole;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CalendarServiceTest {

  @Mock private CalendarRepository calendarRepository;

  @InjectMocks private CalendarService service;

  @Test
  void createCalendarBuildsActiveCalendarForUser() {
    AppUser user = user();
    CreateCalendarRequest request = new CreateCalendarRequest();
    request.setName("Private");
    request.setProvider(CalendarProvider.INTERNAL);
    request.setColor("#4f46e5");
    request.setEditable(true);
    request.setSyncDirection(CalendarSynchronizationType.NONE);

    when(calendarRepository.save(any(Calendar.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    CalendarResponse response = service.createCalendar(request, user);

    assertThat(response.getName()).isEqualTo("Private");
    assertThat(response.getProvider()).isEqualTo(CalendarProvider.INTERNAL);
    assertThat(response.isActive()).isTrue();
    assertThat(response.isEditable()).isTrue();
    verify(calendarRepository).save(any(Calendar.class));
  }

  @Test
  void updateCalendarRejectsNonOwner() {
    AppUser owner = user();
    AppUser stranger = user();
    Calendar calendar = calendar(owner);
    UpdateCalendarRequest request = new UpdateCalendarRequest();
    request.setName("New");

    when(calendarRepository.findById(calendar.getId())).thenReturn(Optional.of(calendar));

    assertThatThrownBy(() -> service.updateCalendar(calendar.getId(), request, stranger))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Not allowed");

    verify(calendarRepository, never()).save(any());
  }

  @Test
  void updateCalendarAppliesOnlyProvidedFields() {
    AppUser user = user();
    Calendar calendar = calendar(user);
    calendar.setName("Old");
    calendar.setColor("#111111");
    calendar.setEditable(false);
    calendar.setActive(true);
    calendar.setSyncDirection(CalendarSynchronizationType.NONE);
    UpdateCalendarRequest request = new UpdateCalendarRequest();
    request.setName("Updated");
    request.setIsActive(false);

    when(calendarRepository.findById(calendar.getId())).thenReturn(Optional.of(calendar));
    when(calendarRepository.save(any(Calendar.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    CalendarResponse response = service.updateCalendar(calendar.getId(), request, user);

    assertThat(response.getName()).isEqualTo("Updated");
    assertThat(response.getColor()).isEqualTo("#111111");
    assertThat(response.isActive()).isFalse();
    assertThat(response.isEditable()).isFalse();
  }

  @Test
  void deleteCalendarRejectsNonOwner() {
    AppUser owner = user();
    AppUser stranger = user();
    Calendar calendar = calendar(owner);
    when(calendarRepository.findById(calendar.getId())).thenReturn(Optional.of(calendar));

    assertThatThrownBy(() -> service.deleteCalendar(calendar.getId(), stranger))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Not allowed");

    verify(calendarRepository, never()).delete(any());
  }

  private Calendar calendar(AppUser user) {
    return Calendar.builder()
        .id(UUID.randomUUID())
        .user(user)
        .name("Calendar")
        .provider(CalendarProvider.INTERNAL)
        .syncDirection(CalendarSynchronizationType.NONE)
        .isActive(true)
        .createdAt(OffsetDateTime.now())
        .updatedAt(OffsetDateTime.now())
        .build();
  }

  private AppUser user() {
    return AppUser.builder()
        .id(UUID.randomUUID())
        .email(UUID.randomUUID() + "@example.com")
        .role(AppUserRole.USER)
        .build();
  }
}
