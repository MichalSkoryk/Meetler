package com.skoryk.projects.meetler.group;

import com.skoryk.projects.meetler.group.dto.CreateGroupRequest;
import com.skoryk.projects.meetler.group.dto.GroupResponse;
import com.skoryk.projects.meetler.group.dto.UpdateGroupRequest;
import com.skoryk.projects.meetler.group.member.*;
import com.skoryk.projects.meetler.subscription.SubscriptionLimitService;
import com.skoryk.projects.meetler.user.AppUser;
import jakarta.transaction.Transactional;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GroupService {

  private final GroupRepository groupRepository;
  private final GroupPermissionService groupPermissionService;
  private final GroupMemberService groupMemberService;
  private final GroupMemberRepository groupMemberRepository;
  private final SubscriptionLimitService subscriptionLimitService;

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

    groupMemberService.addMember(savedGroup.getId(), owner, GroupRole.OWNER);

    return toResponse(savedGroup, GroupRole.OWNER);
  }

  @Transactional
  public List<GroupResponse> getMyGroups(AppUser user) {
    return groupMemberRepository.findByUser(user).stream()
        .map(member -> toResponse(member.getGroup(), member.getRole()))
        .toList();
  }

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

    return toResponse(group, null);
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

  private GroupResponse toResponse(Group group, GroupRole role) {
    return GroupResponse.builder()
        .id(group.getId())
        .name(group.getName())
        .role(role == null ? null : role.name())
        .eventRequiresConfirmation(group.isEventRequiresConfirmation())
        .createdAt(group.getCreatedAt())
        .updatedAt(group.getUpdatedAt())
        .build();
  }
}
