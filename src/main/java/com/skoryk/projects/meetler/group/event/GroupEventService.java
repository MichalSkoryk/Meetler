package com.skoryk.projects.meetler.group.event;

import com.skoryk.projects.meetler.group.Group;
import com.skoryk.projects.meetler.group.GroupRepository;
import com.skoryk.projects.meetler.group.event.dto.*;
import com.skoryk.projects.meetler.group.event.sync.GoogleGroupEventExportService;
import com.skoryk.projects.meetler.group.member.GroupMember;
import com.skoryk.projects.meetler.group.member.GroupMemberRepository;
import com.skoryk.projects.meetler.group.member.GroupPermissionService;
import com.skoryk.projects.meetler.notification.NotificationService;
import com.skoryk.projects.meetler.user.AppUser;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GroupEventService {

  private final GroupRepository groupRepository;
  private final GroupMemberRepository groupMemberRepository;
  private final GroupPermissionService groupPermissionService;
  private final GroupEventRepository eventRepository;
  private final GroupEventParticipantRepository participantRepository;
  private final GoogleGroupEventExportService googleGroupEventExportService;
  private final NotificationService notificationService;
  private final GroupEventMapper groupEventMapper;

  @Transactional
  public GroupEventResponse createEvent(
      UUID groupId, CreateGroupEventRequest request, AppUser creator) {
    validateTimeRange(request.getStartsAt(), request.getEndsAt());
    Group group = getGroup(groupId);
    if (!groupPermissionService.isMember(group, creator)) {
      throw new IllegalArgumentException("Not allowed");
    }

    boolean requiresConfirmation = group.isEventRequiresConfirmation();
    GroupEvent event =
        eventRepository.save(
            GroupEvent.builder()
                .group(group)
                .createdBy(creator)
                .title(request.getTitle())
                .description(request.getDescription())
                .startsAt(request.getStartsAt())
                .endsAt(request.getEndsAt())
                .requiresConfirmation(requiresConfirmation)
                .status(
                    requiresConfirmation
                        ? GroupEventStatus.PENDING_CONFIRMATION
                        : GroupEventStatus.CONFIRMED)
                .build());

    GroupEventParticipantStatus initialStatus =
        requiresConfirmation
            ? GroupEventParticipantStatus.PENDING
            : GroupEventParticipantStatus.ACCEPTED;
    OffsetDateTime respondedAt = requiresConfirmation ? null : OffsetDateTime.now();

    List<GroupEventParticipant> participants =
        groupMemberRepository.findByGroup(group).stream()
            .map(GroupMember::getUser)
            .map(
                member ->
                    GroupEventParticipant.builder()
                        .groupEvent(event)
                        .user(member)
                        .status(initialStatus)
                        .respondedAt(respondedAt)
                        .build())
            .map(participantRepository::save)
            .toList();

    notificationService.notifyGroupEventCreated(event, participants);

    return toResponse(event);
  }

  public List<GroupEventResponse> listEvents(
      UUID groupId, OffsetDateTime from, OffsetDateTime to, AppUser user) {
    validateTimeRange(from, to);
    Group group = getGroup(groupId);
    if (!groupPermissionService.isMember(group, user)) {
      throw new IllegalArgumentException("Not allowed");
    }

    return eventRepository.findByGroupOverlapping(group, from, to).stream()
        .map(this::toResponse)
        .toList();
  }

  @Transactional
  public GroupEventResponse updateEvent(
      UUID groupId, UUID eventId, UpdateGroupEventRequest request, AppUser user) {
    validateTimeRange(request.getStartsAt(), request.getEndsAt());
    Group group = getGroup(groupId);
    GroupEvent groupEvent =
        eventRepository
            .findByIdAndGroup(eventId, group)
            .orElseThrow(() -> new IllegalArgumentException("Event not found"));

    if (!groupPermissionService.isAdmin(group, user)
        && !groupEvent.getCreatedBy().getId().equals(user.getId())) {
      throw new IllegalArgumentException("Not allowed");
    }

    if (groupEvent.getStatus() == GroupEventStatus.CANCELLED) {
      throw new IllegalStateException("Cancelled events cannot be changed");
    }

    boolean timeChanged =
        !request.getStartsAt().isEqual(groupEvent.getStartsAt())
            || !request.getEndsAt().isEqual(groupEvent.getEndsAt());

    groupEvent.setTitle(request.getTitle());
    groupEvent.setDescription(request.getDescription());

    if (timeChanged) {
      groupEvent.setStartsAt(request.getStartsAt());
      groupEvent.setEndsAt(request.getEndsAt());
      GroupEventStatus groupEventStatus =
          groupEvent.isRequiresConfirmation()
              ? GroupEventStatus.PENDING_CONFIRMATION
              : GroupEventStatus.CONFIRMED;
      groupEvent.setStatus(groupEventStatus);

      participantRepository.setParticipantStateInEvent(
          groupEvent,
          groupEventStatus == GroupEventStatus.CONFIRMED
              ? GroupEventParticipantStatus.ACCEPTED
              : GroupEventParticipantStatus.PENDING,
          groupEventStatus == GroupEventStatus.CONFIRMED ? OffsetDateTime.now() : null);
    }

    eventRepository.save(groupEvent);
    googleGroupEventExportService.synchronizeExistingGoogleExports(groupEvent);
    notificationService.notifyGroupEventUpdated(
        groupEvent, participantRepository.findByGroupEvent(groupEvent));

    return toResponse(groupEvent);
  }

  @Transactional
  public GroupEventResponse respond(
      UUID groupId, UUID eventId, RespondToGroupEventRequest request, AppUser user) {
    Group group = getGroup(groupId);
    if (!groupPermissionService.isMember(group, user)) {
      throw new IllegalArgumentException("Not allowed");
    }

    GroupEvent event =
        eventRepository
            .findByIdAndGroup(eventId, group)
            .orElseThrow(() -> new IllegalArgumentException("Group event not found"));
    if (!event.isRequiresConfirmation()) {
      throw new IllegalStateException("This event does not require confirmation");
    }
    if (event.getStatus() == GroupEventStatus.CANCELLED) {
      throw new IllegalStateException("Cancelled events cannot be changed");
    }
    if (request.getStatus() == GroupEventParticipantStatus.PENDING) {
      throw new IllegalArgumentException("Response status must be ACCEPTED or DECLINED");
    }

    GroupEventParticipant participant =
        participantRepository
            .findByGroupEventAndUser(event, user)
            .orElseThrow(() -> new IllegalArgumentException("Event participant not found"));
    participant.setStatus(request.getStatus());
    participant.setRespondedAt(OffsetDateTime.now());
    participantRepository.save(participant);

    boolean wasPendingConfirmation = event.getStatus() == GroupEventStatus.PENDING_CONFIRMATION;
    if (wasPendingConfirmation && allParticipantsAccepted(event)) {
      event.setStatus(GroupEventStatus.CONFIRMED);
      eventRepository.save(event);
      notificationService.notifyGroupEventConfirmed(
          event, participantRepository.findByGroupEvent(event));
    }

    return toResponse(event);
  }

  @Transactional
  public void cancelEvent(UUID groupId, UUID eventId, AppUser user) {
    Group group = getGroup(groupId);
    GroupEvent groupEvent =
        eventRepository
            .findByIdAndGroup(eventId, group)
            .orElseThrow(() -> new IllegalArgumentException("Group event not found"));

    if (!groupEvent.getCreatedBy().getId().equals(user.getId())
        && !groupPermissionService.isAdmin(group, user)) {
      throw new IllegalArgumentException("Not allowed");
    }

    if (groupEvent.getStatus() == GroupEventStatus.CANCELLED) {
      return;
    }

    groupEvent.setStatus(GroupEventStatus.CANCELLED);
    eventRepository.save(groupEvent);
    googleGroupEventExportService.deleteExistingGoogleExports(groupEvent);
    notificationService.notifyGroupEventCancelled(
        groupEvent, participantRepository.findByGroupEvent(groupEvent), user);
  }

  private boolean allParticipantsAccepted(GroupEvent event) {
    return participantRepository.findByGroupEvent(event).stream()
        .allMatch(participant -> participant.getStatus() == GroupEventParticipantStatus.ACCEPTED);
  }

  private Group getGroup(UUID groupId) {
    return groupRepository
        .findById(groupId)
        .orElseThrow(() -> new IllegalArgumentException("Group not found"));
  }

  private void validateTimeRange(OffsetDateTime startsAt, OffsetDateTime endsAt) {
    if (startsAt == null || endsAt == null) {
      throw new IllegalArgumentException("Event requires start and end time");
    }
    if (!endsAt.isAfter(startsAt)) {
      throw new IllegalArgumentException("Event end must be after start");
    }
  }

  private GroupEventResponse toResponse(GroupEvent event) {
    return groupEventMapper.toResponse(event, participantRepository.findByGroupEvent(event));
  }
}
