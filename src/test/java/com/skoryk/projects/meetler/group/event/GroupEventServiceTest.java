package com.skoryk.projects.meetler.group.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.skoryk.projects.meetler.group.Group;
import com.skoryk.projects.meetler.group.GroupRepository;
import com.skoryk.projects.meetler.group.event.dto.CreateGroupEventRequest;
import com.skoryk.projects.meetler.group.event.dto.GroupEventResponse;
import com.skoryk.projects.meetler.group.event.dto.RespondToGroupEventRequest;
import com.skoryk.projects.meetler.group.event.dto.UpdateGroupEventRequest;
import com.skoryk.projects.meetler.group.event.sync.GoogleGroupEventExportService;
import com.skoryk.projects.meetler.group.member.GroupMember;
import com.skoryk.projects.meetler.group.member.GroupMemberRepository;
import com.skoryk.projects.meetler.group.member.GroupPermissionService;
import com.skoryk.projects.meetler.group.member.GroupRole;
import com.skoryk.projects.meetler.notification.NotificationService;
import com.skoryk.projects.meetler.user.AppUser;
import com.skoryk.projects.meetler.user.AppUserRole;
import java.time.OffsetDateTime;
import java.util.List;
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

@ExtendWith(MockitoExtension.class)
class GroupEventServiceTest {

  @Mock private GroupRepository groupRepository;
  @Mock private GroupMemberRepository groupMemberRepository;
  @Mock private GroupPermissionService groupPermissionService;
  @Mock private GroupEventRepository eventRepository;
  @Mock private GroupEventParticipantRepository participantRepository;
  @Mock private GoogleGroupEventExportService googleGroupEventExportService;
  @Mock private NotificationService notificationService;
  @Spy private GroupEventMapper groupEventMapper = Mappers.getMapper(GroupEventMapper.class);

  @InjectMocks private GroupEventService service;

  @Test
  void createEventUsesPendingConfirmationWhenGroupRequiresConfirmation() {
    AppUser creator = user("creator@example.com");
    AppUser member = user("member@example.com");
    Group group = group(true);
    when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));
    when(groupPermissionService.isMember(group, creator)).thenReturn(true);
    when(eventRepository.save(any(GroupEvent.class)))
        .thenAnswer(invocation -> savedEvent(invocation.getArgument(0)));
    when(groupMemberRepository.findByGroup(group))
        .thenReturn(
            List.of(
                member(group, creator, GroupRole.OWNER), member(group, member, GroupRole.MEMBER)));
    when(participantRepository.findByGroupEvent(any())).thenReturn(List.of());

    GroupEventResponse response = service.createEvent(group.getId(), eventRequest(), creator);

    assertThat(response.getStatus()).isEqualTo("PENDING_CONFIRMATION");
    assertThat(response.isRequiresConfirmation()).isTrue();

    ArgumentCaptor<GroupEventParticipant> participantCaptor =
        ArgumentCaptor.forClass(GroupEventParticipant.class);
    verify(participantRepository, times(2)).save(participantCaptor.capture());
    assertThat(participantCaptor.getAllValues())
        .allMatch(participant -> participant.getStatus() == GroupEventParticipantStatus.PENDING);
    verify(notificationService).notifyGroupEventCreated(any(GroupEvent.class), any());
  }

  @Test
  void createEventConfirmsImmediatelyWhenGroupDoesNotRequireConfirmation() {
    AppUser creator = user("creator@example.com");
    Group group = group(false);
    when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));
    when(groupPermissionService.isMember(group, creator)).thenReturn(true);
    when(eventRepository.save(any(GroupEvent.class)))
        .thenAnswer(invocation -> savedEvent(invocation.getArgument(0)));
    when(groupMemberRepository.findByGroup(group))
        .thenReturn(List.of(member(group, creator, GroupRole.OWNER)));
    when(participantRepository.findByGroupEvent(any())).thenReturn(List.of());

    GroupEventResponse response = service.createEvent(group.getId(), eventRequest(), creator);

    assertThat(response.getStatus()).isEqualTo("CONFIRMED");
    assertThat(response.isRequiresConfirmation()).isFalse();

    ArgumentCaptor<GroupEventParticipant> participantCaptor =
        ArgumentCaptor.forClass(GroupEventParticipant.class);
    verify(participantRepository).save(participantCaptor.capture());
    assertThat(participantCaptor.getValue().getStatus())
        .isEqualTo(GroupEventParticipantStatus.ACCEPTED);
    assertThat(participantCaptor.getValue().getRespondedAt()).isNotNull();
    verify(notificationService).notifyGroupEventCreated(any(GroupEvent.class), any());
  }

  @Test
  void respondConfirmsEventWhenAllParticipantsAccepted() {
    AppUser user = user("member@example.com");
    Group group = group(true);
    GroupEvent event =
        GroupEvent.builder()
            .id(UUID.randomUUID())
            .group(group)
            .createdBy(user)
            .title("Dinner")
            .startsAt(OffsetDateTime.parse("2026-06-10T18:00:00+02:00"))
            .endsAt(OffsetDateTime.parse("2026-06-10T19:00:00+02:00"))
            .requiresConfirmation(true)
            .status(GroupEventStatus.PENDING_CONFIRMATION)
            .build();
    GroupEventParticipant participant =
        GroupEventParticipant.builder()
            .groupEvent(event)
            .user(user)
            .status(GroupEventParticipantStatus.PENDING)
            .build();
    RespondToGroupEventRequest request = new RespondToGroupEventRequest();
    request.setStatus(GroupEventParticipantStatus.ACCEPTED);

    when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));
    when(groupPermissionService.isMember(group, user)).thenReturn(true);
    when(eventRepository.findByIdAndGroup(event.getId(), group)).thenReturn(Optional.of(event));
    when(participantRepository.findByGroupEventAndUser(event, user))
        .thenReturn(Optional.of(participant));
    when(participantRepository.findByGroupEvent(event)).thenReturn(List.of(participant));

    GroupEventResponse response = service.respond(group.getId(), event.getId(), request, user);

    assertThat(response.getStatus()).isEqualTo("CONFIRMED");
    verify(eventRepository).save(event);
    verify(notificationService).notifyGroupEventConfirmed(event, List.of(participant));
  }

  @Test
  void respondDoesNotNotifyConfirmationAgainWhenEventIsAlreadyConfirmed() {
    AppUser user = user("member@example.com");
    Group group = group(true);
    GroupEvent event =
        GroupEvent.builder()
            .id(UUID.randomUUID())
            .group(group)
            .createdBy(user)
            .title("Dinner")
            .startsAt(OffsetDateTime.parse("2026-06-10T18:00:00+02:00"))
            .endsAt(OffsetDateTime.parse("2026-06-10T19:00:00+02:00"))
            .requiresConfirmation(true)
            .status(GroupEventStatus.CONFIRMED)
            .build();
    GroupEventParticipant participant =
        GroupEventParticipant.builder()
            .groupEvent(event)
            .user(user)
            .status(GroupEventParticipantStatus.ACCEPTED)
            .build();
    RespondToGroupEventRequest request = new RespondToGroupEventRequest();
    request.setStatus(GroupEventParticipantStatus.ACCEPTED);

    when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));
    when(groupPermissionService.isMember(group, user)).thenReturn(true);
    when(eventRepository.findByIdAndGroup(event.getId(), group)).thenReturn(Optional.of(event));
    when(participantRepository.findByGroupEventAndUser(event, user))
        .thenReturn(Optional.of(participant));
    when(participantRepository.findByGroupEvent(event)).thenReturn(List.of(participant));

    GroupEventResponse response = service.respond(group.getId(), event.getId(), request, user);

    assertThat(response.getStatus()).isEqualTo("CONFIRMED");
    verify(eventRepository, never()).save(event);
    verify(notificationService, never()).notifyGroupEventConfirmed(any(), any());
  }

  @Test
  void updateEventAllowsCreatorToChangeTitleWithoutResettingParticipants() {
    AppUser creator = user("creator@example.com");
    Group group = group(true);
    GroupEvent event = event(group, creator, true, GroupEventStatus.CONFIRMED);
    UpdateGroupEventRequest request =
        updateRequest("Updated dinner", "New description", event.getStartsAt(), event.getEndsAt());

    when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));
    when(eventRepository.findByIdAndGroup(event.getId(), group)).thenReturn(Optional.of(event));
    when(participantRepository.findByGroupEvent(event)).thenReturn(List.of());

    GroupEventResponse response =
        service.updateEvent(group.getId(), event.getId(), request, creator);

    assertThat(response.getTitle()).isEqualTo("Updated dinner");
    assertThat(response.getDescription()).isEqualTo("New description");
    assertThat(response.getStatus()).isEqualTo("CONFIRMED");
    verify(participantRepository, never())
        .setParticipantStateInEvent(
            any(GroupEvent.class), any(GroupEventParticipantStatus.class), any());
    verify(eventRepository).save(event);
    verify(notificationService).notifyGroupEventUpdated(event, List.of());
  }

  @Test
  void updateEventResetsParticipantsWhenTimeChangesAndConfirmationIsRequired() {
    AppUser creator = user("creator@example.com");
    Group group = group(true);
    GroupEvent event = event(group, creator, true, GroupEventStatus.CONFIRMED);
    OffsetDateTime newStart = event.getStartsAt().plusDays(1);
    OffsetDateTime newEnd = event.getEndsAt().plusDays(1);
    UpdateGroupEventRequest request = updateRequest("Dinner", "Moved", newStart, newEnd);

    when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));
    when(eventRepository.findByIdAndGroup(event.getId(), group)).thenReturn(Optional.of(event));
    when(participantRepository.findByGroupEvent(event)).thenReturn(List.of());

    GroupEventResponse response =
        service.updateEvent(group.getId(), event.getId(), request, creator);

    assertThat(response.getStartsAt()).isEqualTo(newStart);
    assertThat(response.getEndsAt()).isEqualTo(newEnd);
    assertThat(response.getStatus()).isEqualTo("PENDING_CONFIRMATION");
    verify(participantRepository)
        .setParticipantStateInEvent(event, GroupEventParticipantStatus.PENDING, null);
    verify(eventRepository).save(event);
    verify(notificationService).notifyGroupEventUpdated(event, List.of());
  }

  @Test
  void updateEventKeepsConfirmedStatusWhenTimeChangesWithoutConfirmation() {
    AppUser creator = user("creator@example.com");
    Group group = group(false);
    GroupEvent event = event(group, creator, false, GroupEventStatus.CONFIRMED);
    UpdateGroupEventRequest request =
        updateRequest(
            "Dinner", "Moved", event.getStartsAt().plusHours(1), event.getEndsAt().plusHours(1));

    when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));
    when(groupPermissionService.isAdmin(group, creator)).thenReturn(false);
    when(eventRepository.findByIdAndGroup(event.getId(), group)).thenReturn(Optional.of(event));
    when(participantRepository.findByGroupEvent(event)).thenReturn(List.of());

    GroupEventResponse response =
        service.updateEvent(group.getId(), event.getId(), request, creator);

    assertThat(response.getStatus()).isEqualTo("CONFIRMED");
    verify(participantRepository)
        .setParticipantStateInEvent(
            eq(event), eq(GroupEventParticipantStatus.ACCEPTED), any(OffsetDateTime.class));
    verify(eventRepository).save(event);
    verify(googleGroupEventExportService).synchronizeExistingGoogleExports(event);
    verify(notificationService).notifyGroupEventUpdated(event, List.of());
  }

  @Test
  void updateEventAllowsAdminToChangeAnotherUsersEvent() {
    AppUser creator = user("creator@example.com");
    AppUser admin = user("admin@example.com");
    Group group = group(true);
    GroupEvent event = event(group, creator, true, GroupEventStatus.PENDING_CONFIRMATION);
    UpdateGroupEventRequest request =
        updateRequest("Admin update", null, event.getStartsAt(), event.getEndsAt());

    when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));
    when(groupPermissionService.isAdmin(group, admin)).thenReturn(true);
    when(eventRepository.findByIdAndGroup(event.getId(), group)).thenReturn(Optional.of(event));
    when(participantRepository.findByGroupEvent(event)).thenReturn(List.of());

    GroupEventResponse response = service.updateEvent(group.getId(), event.getId(), request, admin);

    assertThat(response.getTitle()).isEqualTo("Admin update");
    verify(eventRepository).save(event);
    verify(notificationService).notifyGroupEventUpdated(event, List.of());
  }

  @Test
  void updateEventRejectsUserWhoIsNotCreatorOrAdmin() {
    AppUser creator = user("creator@example.com");
    AppUser otherUser = user("other@example.com");
    Group group = group(true);
    GroupEvent event = event(group, creator, true, GroupEventStatus.PENDING_CONFIRMATION);

    when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));
    when(groupPermissionService.isAdmin(group, otherUser)).thenReturn(false);
    when(eventRepository.findByIdAndGroup(event.getId(), group)).thenReturn(Optional.of(event));

    assertThatThrownBy(
            () ->
                service.updateEvent(
                    group.getId(),
                    event.getId(),
                    updateRequest("Blocked", null, event.getStartsAt(), event.getEndsAt()),
                    otherUser))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Not allowed");

    verify(eventRepository, never()).save(any(GroupEvent.class));
  }

  @Test
  void updateEventRejectsCancelledEvent() {
    AppUser creator = user("creator@example.com");
    Group group = group(true);
    GroupEvent event = event(group, creator, true, GroupEventStatus.CANCELLED);

    when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));
    when(groupPermissionService.isAdmin(group, creator)).thenReturn(false);
    when(eventRepository.findByIdAndGroup(event.getId(), group)).thenReturn(Optional.of(event));

    assertThatThrownBy(
            () ->
                service.updateEvent(
                    group.getId(),
                    event.getId(),
                    updateRequest("Blocked", null, event.getStartsAt(), event.getEndsAt()),
                    creator))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Cancelled events cannot be changed");

    verify(eventRepository, never()).save(any(GroupEvent.class));
    verify(participantRepository, never())
        .setParticipantStateInEvent(eq(event), any(GroupEventParticipantStatus.class), any());
  }

  @Test
  void cancelEventAllowsCreatorToCancelOwnEvent() {
    AppUser creator = user("creator@example.com");
    Group group = group(true);
    GroupEvent event = event(group, creator, true, GroupEventStatus.PENDING_CONFIRMATION);

    when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));
    when(eventRepository.findByIdAndGroup(event.getId(), group)).thenReturn(Optional.of(event));

    service.cancelEvent(group.getId(), event.getId(), creator);

    assertThat(event.getStatus()).isEqualTo(GroupEventStatus.CANCELLED);
    verify(eventRepository).save(event);
    verify(googleGroupEventExportService).deleteExistingGoogleExports(event);
    verify(notificationService).notifyGroupEventCancelled(event, List.of(), creator);
    verify(participantRepository, never())
        .setParticipantStateInEvent(
            any(GroupEvent.class), any(GroupEventParticipantStatus.class), any());
  }

  @Test
  void cancelEventAllowsAdminToCancelAnotherUsersEvent() {
    AppUser creator = user("creator@example.com");
    AppUser admin = user("admin@example.com");
    Group group = group(true);
    GroupEvent event = event(group, creator, true, GroupEventStatus.CONFIRMED);

    when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));
    when(groupPermissionService.isAdmin(group, admin)).thenReturn(true);
    when(eventRepository.findByIdAndGroup(event.getId(), group)).thenReturn(Optional.of(event));

    service.cancelEvent(group.getId(), event.getId(), admin);

    assertThat(event.getStatus()).isEqualTo(GroupEventStatus.CANCELLED);
    verify(eventRepository).save(event);
    verify(notificationService).notifyGroupEventCancelled(event, List.of(), admin);
  }

  @Test
  void cancelEventRejectsUserWhoIsNotCreatorOrAdmin() {
    AppUser creator = user("creator@example.com");
    AppUser otherUser = user("other@example.com");
    Group group = group(true);
    GroupEvent event = event(group, creator, true, GroupEventStatus.CONFIRMED);

    when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));
    when(groupPermissionService.isAdmin(group, otherUser)).thenReturn(false);
    when(eventRepository.findByIdAndGroup(event.getId(), group)).thenReturn(Optional.of(event));

    assertThatThrownBy(() -> service.cancelEvent(group.getId(), event.getId(), otherUser))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Not allowed");

    assertThat(event.getStatus()).isEqualTo(GroupEventStatus.CONFIRMED);
    verify(eventRepository, never()).save(any(GroupEvent.class));
  }

  @Test
  void cancelEventIsIdempotentWhenEventIsAlreadyCancelled() {
    AppUser creator = user("creator@example.com");
    Group group = group(true);
    GroupEvent event = event(group, creator, true, GroupEventStatus.CANCELLED);

    when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));
    when(eventRepository.findByIdAndGroup(event.getId(), group)).thenReturn(Optional.of(event));

    service.cancelEvent(group.getId(), event.getId(), creator);

    assertThat(event.getStatus()).isEqualTo(GroupEventStatus.CANCELLED);
    verify(eventRepository, never()).save(any(GroupEvent.class));
  }

  private GroupEvent savedEvent(GroupEvent event) {
    event.setId(UUID.randomUUID());
    return event;
  }

  private CreateGroupEventRequest eventRequest() {
    CreateGroupEventRequest request = new CreateGroupEventRequest();
    request.setTitle("Dinner");
    request.setStartsAt(OffsetDateTime.parse("2026-06-10T18:00:00+02:00"));
    request.setEndsAt(OffsetDateTime.parse("2026-06-10T19:00:00+02:00"));
    return request;
  }

  private UpdateGroupEventRequest updateRequest(
      String title, String description, OffsetDateTime startsAt, OffsetDateTime endsAt) {
    UpdateGroupEventRequest request = new UpdateGroupEventRequest();
    request.setTitle(title);
    request.setDescription(description);
    request.setStartsAt(startsAt);
    request.setEndsAt(endsAt);
    return request;
  }

  private GroupEvent event(
      Group group, AppUser creator, boolean requiresConfirmation, GroupEventStatus status) {
    return GroupEvent.builder()
        .id(UUID.randomUUID())
        .group(group)
        .createdBy(creator)
        .title("Dinner")
        .description("Original description")
        .startsAt(OffsetDateTime.parse("2026-06-10T18:00:00+02:00"))
        .endsAt(OffsetDateTime.parse("2026-06-10T19:00:00+02:00"))
        .requiresConfirmation(requiresConfirmation)
        .status(status)
        .build();
  }

  private Group group(boolean eventRequiresConfirmation) {
    return Group.builder()
        .id(UUID.randomUUID())
        .name("Friends")
        .eventRequiresConfirmation(eventRequiresConfirmation)
        .createdAt(OffsetDateTime.now())
        .updatedAt(OffsetDateTime.now())
        .build();
  }

  private GroupMember member(Group group, AppUser user, GroupRole role) {
    return GroupMember.builder().id(UUID.randomUUID()).group(group).user(user).role(role).build();
  }

  private AppUser user(String email) {
    return AppUser.builder().id(UUID.randomUUID()).email(email).role(AppUserRole.USER).build();
  }
}
