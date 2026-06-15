package com.skoryk.projects.meetler.group.event.sync;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.skoryk.projects.meetler.calendar.CalendarProvider;
import com.skoryk.projects.meetler.calendar.external.ExternalCalendarAccount;
import com.skoryk.projects.meetler.calendar.external.ExternalCalendarAccountRepository;
import com.skoryk.projects.meetler.group.Group;
import com.skoryk.projects.meetler.group.GroupRepository;
import com.skoryk.projects.meetler.group.event.GroupEvent;
import com.skoryk.projects.meetler.group.event.GroupEventRepository;
import com.skoryk.projects.meetler.group.event.GroupEventStatus;
import com.skoryk.projects.meetler.group.member.GroupPermissionService;
import com.skoryk.projects.meetler.user.AppUser;
import com.skoryk.projects.meetler.user.AppUserRole;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GoogleGroupEventExportServiceTest {

  @Mock private GroupRepository groupRepository;
  @Mock private GroupEventRepository groupEventRepository;
  @Mock private GroupPermissionService groupPermissionService;
  @Mock private ExternalCalendarAccountRepository externalCalendarAccountRepository;

  @InjectMocks private GoogleGroupEventExportService service;

  @Test
  void exportToGoogleRequiresGroupMembership() {
    AppUser user = user();
    Group group = group();
    UUID eventId = UUID.randomUUID();
    when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));
    when(groupPermissionService.isMember(group, user)).thenReturn(false);

    assertThatThrownBy(() -> service.exportToGoogle(group.getId(), eventId, user))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Not allowed");
  }

  @Test
  void exportToGoogleRejectsCancelledEvent() {
    AppUser user = user();
    Group group = group();
    GroupEvent event = event(group, user, GroupEventStatus.CANCELLED);
    when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));
    when(groupPermissionService.isMember(group, user)).thenReturn(true);
    when(groupEventRepository.findByIdAndGroup(event.getId(), group))
        .thenReturn(Optional.of(event));

    assertThatThrownBy(() -> service.exportToGoogle(group.getId(), event.getId(), user))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Cancelled events cannot be exported");
  }

  @Test
  void exportToGoogleRejectsPendingEvent() {
    AppUser user = user();
    Group group = group();
    GroupEvent event = event(group, user, GroupEventStatus.PENDING_CONFIRMATION);
    when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));
    when(groupPermissionService.isMember(group, user)).thenReturn(true);
    when(groupEventRepository.findByIdAndGroup(event.getId(), group))
        .thenReturn(Optional.of(event));

    assertThatThrownBy(() -> service.exportToGoogle(group.getId(), event.getId(), user))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Only confirmed events can be exported");
  }

  @Test
  void exportToGoogleRequiresConnectedGoogleAccount() {
    AppUser user = user();
    Group group = group();
    GroupEvent event = event(group, user, GroupEventStatus.CONFIRMED);
    when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));
    when(groupPermissionService.isMember(group, user)).thenReturn(true);
    when(groupEventRepository.findByIdAndGroup(event.getId(), group))
        .thenReturn(Optional.of(event));
    when(externalCalendarAccountRepository.findByUserAndProviderAndRevokedAtIsNull(
            user, CalendarProvider.GOOGLE))
        .thenReturn(List.of());

    assertThatThrownBy(() -> service.exportToGoogle(group.getId(), event.getId(), user))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("No connected Google account found");
  }

  @Test
  void exportToGoogleRequiresCalendarWriteScope() {
    AppUser user = user();
    Group group = group();
    GroupEvent event = event(group, user, GroupEventStatus.CONFIRMED);
    ExternalCalendarAccount account =
        ExternalCalendarAccount.builder()
            .user(user)
            .provider(CalendarProvider.GOOGLE)
            .externalAccountId("google-account")
            .scopes("openid email profile https://www.googleapis.com/auth/calendar.events")
            .build();
    when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));
    when(groupPermissionService.isMember(group, user)).thenReturn(true);
    when(groupEventRepository.findByIdAndGroup(event.getId(), group))
        .thenReturn(Optional.of(event));
    when(externalCalendarAccountRepository.findByUserAndProviderAndRevokedAtIsNull(
            user, CalendarProvider.GOOGLE))
        .thenReturn(List.of(account));

    assertThatThrownBy(() -> service.exportToGoogle(group.getId(), event.getId(), user))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage(
            "Connected Google account is missing Meetler calendar export scope. Reconnect Google Calendar.");
  }

  private Group group() {
    return Group.builder()
        .id(UUID.randomUUID())
        .name("Friends")
        .createdAt(OffsetDateTime.now())
        .updatedAt(OffsetDateTime.now())
        .build();
  }

  private GroupEvent event(Group group, AppUser user, GroupEventStatus status) {
    return GroupEvent.builder()
        .id(UUID.randomUUID())
        .group(group)
        .createdBy(user)
        .title("Dinner")
        .startsAt(OffsetDateTime.parse("2026-06-10T18:00:00+02:00"))
        .endsAt(OffsetDateTime.parse("2026-06-10T19:00:00+02:00"))
        .requiresConfirmation(false)
        .status(status)
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
