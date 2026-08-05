package com.skoryk.projects.meetler.group;

import com.skoryk.projects.meetler.group.dto.CreateGroupRequest;
import com.skoryk.projects.meetler.group.dto.GroupEventSummaryResponse;
import com.skoryk.projects.meetler.group.dto.GroupResponse;
import com.skoryk.projects.meetler.group.dto.UpdateGroupRequest;
import com.skoryk.projects.meetler.group.event.GroupEvent;
import com.skoryk.projects.meetler.group.event.GroupEventParticipant;
import com.skoryk.projects.meetler.group.event.GroupEventParticipantRepository;
import com.skoryk.projects.meetler.group.event.GroupEventParticipantStatus;
import com.skoryk.projects.meetler.group.event.GroupEventRepository;
import com.skoryk.projects.meetler.group.event.GroupEventStatus;
import com.skoryk.projects.meetler.group.member.*;
import com.skoryk.projects.meetler.subscription.SubscriptionLimitService;
import com.skoryk.projects.meetler.user.AppUser;
import jakarta.transaction.Transactional;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GroupService {

  private final GroupRepository groupRepository;
  private final GroupPermissionService groupPermissionService;
  private final GroupMemberService groupMemberService;
  private final GroupMemberRepository groupMemberRepository;
  private final GroupEventRepository groupEventRepository;
  private final GroupEventParticipantRepository groupEventParticipantRepository;
  private final SubscriptionLimitService subscriptionLimitService;
  private final GroupMapper groupMapper;

  public GroupResponse createGroup(CreateGroupRequest request, AppUser owner) {

    subscriptionLimitService.assertCanCreateGroup(owner);

    Group group =
        Group.builder()
            .name(request.getName())
            .eventRequiresConfirmation(
                request.getEventRequiresConfirmation() == null
                    || request.getEventRequiresConfirmation())
            .createdAt(OffsetDateTime.now())
            .updatedAt(OffsetDateTime.now())
            .build();

    Group savedGroup = groupRepository.save(group);

    GroupMember ownerMembership =
        groupMemberService.addMember(savedGroup.getId(), owner, GroupRole.OWNER);

    return toResponse(ownerMembership, 1, List.of(), Map.of());
  }

  @Transactional
  public List<GroupResponse> getMyGroups(AppUser user) {
    return buildResponses(groupMemberRepository.findByUser(user), user);
  }

  @Transactional
  public GroupResponse updateGroup(UUID groupId, UpdateGroupRequest request, AppUser user) {
    Group group =
        groupRepository
            .findById(groupId)
            .orElseThrow(() -> new IllegalArgumentException("Group not found"));

    if (!groupPermissionService.isAdmin(group, user)) {
      throw new IllegalArgumentException("Not allowed");
    }

    group.setName(request.getName());
    if (request.getEventRequiresConfirmation() != null) {
      group.setEventRequiresConfirmation(request.getEventRequiresConfirmation());
    }
    group.setUpdatedAt(OffsetDateTime.now());

    groupRepository.save(group);

    GroupMember membership =
        groupMemberRepository
            .findByGroupAndUser(group, user)
            .orElseThrow(() -> new IllegalArgumentException("User not in group"));
    return buildResponses(List.of(membership), user).getFirst();
  }

  public void deleteGroup(UUID groupId, AppUser user) {
    Group group =
        groupRepository
            .findById(groupId)
            .orElseThrow(() -> new IllegalArgumentException("Group not found"));

    if (!groupPermissionService.isOwner(group, user)) {
      throw new IllegalArgumentException("Not allowed");
    }

    groupRepository.delete(group);
  }

  private List<GroupResponse> buildResponses(List<GroupMember> memberships, AppUser user) {
    if (memberships.isEmpty()) {
      return List.of();
    }

    List<Group> groups = memberships.stream().map(GroupMember::getGroup).toList();
    Map<UUID, Long> memberCounts =
        groupMemberRepository.findByGroupIn(groups).stream()
            .collect(
                Collectors.groupingBy(member -> member.getGroup().getId(), Collectors.counting()));
    List<GroupEvent> upcomingEvents =
        groupEventRepository.findUpcomingByGroups(
            groups, OffsetDateTime.now(), GroupEventStatus.CANCELLED);
    Map<UUID, GroupEventParticipant> currentUserParticipants =
        participantsByEvent(upcomingEvents, user);
    Map<UUID, List<GroupEvent>> eventsByGroup =
        upcomingEvents.stream().collect(Collectors.groupingBy(event -> event.getGroup().getId()));

    return memberships.stream()
        .map(
            membership ->
                toResponse(
                    membership,
                    memberCounts.getOrDefault(membership.getGroup().getId(), 0L),
                    eventsByGroup.getOrDefault(membership.getGroup().getId(), List.of()),
                    currentUserParticipants))
        .toList();
  }

  private Map<UUID, GroupEventParticipant> participantsByEvent(
      Collection<GroupEvent> events, AppUser user) {
    if (events.isEmpty()) {
      return Map.of();
    }

    return groupEventParticipantRepository.findByGroupEventInAndUser(events, user).stream()
        .collect(
            Collectors.toMap(
                participant -> participant.getGroupEvent().getId(), Function.identity()));
  }

  private GroupResponse toResponse(
      GroupMember membership,
      long memberCount,
      List<GroupEvent> upcomingEvents,
      Map<UUID, GroupEventParticipant> currentUserParticipants) {
    List<GroupEvent> pendingEvents =
        upcomingEvents.stream()
            .filter(GroupEvent::isRequiresConfirmation)
            .filter(
                event -> {
                  GroupEventParticipant participant = currentUserParticipants.get(event.getId());
                  return participant != null
                      && participant.getStatus() == GroupEventParticipantStatus.PENDING;
                })
            .toList();

    GroupEvent nextEvent = upcomingEvents.isEmpty() ? null : upcomingEvents.getFirst();
    GroupEvent nextPendingEvent = pendingEvents.isEmpty() ? null : pendingEvents.getFirst();

    return groupMapper.toResponse(
        membership,
        memberCount,
        pendingEvents.size(),
        toEventSummary(nextEvent, currentUserParticipants),
        toEventSummary(nextPendingEvent, currentUserParticipants));
  }

  private GroupEventSummaryResponse toEventSummary(
      GroupEvent event, Map<UUID, GroupEventParticipant> currentUserParticipants) {
    if (event == null) {
      return null;
    }
    return groupMapper.toEventSummary(event, currentUserParticipants.get(event.getId()));
  }
}
