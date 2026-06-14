package com.skoryk.projects.meetler.group.member;

import com.skoryk.projects.meetler.group.Group;
import com.skoryk.projects.meetler.group.GroupRepository;
import com.skoryk.projects.meetler.subscription.SubscriptionLimitService;
import com.skoryk.projects.meetler.user.AppUser;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GroupAdminService {
  private final GroupRepository groupRepository;
  private final GroupMemberRepository memberRepository;
  private final GroupPermissionService permissionService;
  private final SubscriptionLimitService subscriptionLimitService;

  public void promoteToAdmin(UUID groupId, UUID userId, AppUser requester) {
    Group group =
        groupRepository
            .findById(groupId)
            .orElseThrow(() -> new IllegalArgumentException("Group not found"));

    if (!permissionService.isOwner(group, requester)) {
      throw new IllegalArgumentException("Only owner can promote admins");
    }

    GroupMember member =
        memberRepository
            .findByGroupIdAndUserId(groupId, userId)
            .orElseThrow(() -> new IllegalArgumentException("User not in group"));

    if (member.getRole() != GroupRole.MEMBER) {
      throw new IllegalArgumentException("Only members can be promoted to admin");
    }

    member.setRole(GroupRole.ADMIN);
    memberRepository.save(member);
  }

  public void demoteAdmin(UUID groupId, UUID userId, AppUser requester) {
    Group group =
        groupRepository
            .findById(groupId)
            .orElseThrow(() -> new IllegalArgumentException("Group not found"));

    if (!permissionService.isOwner(group, requester)) {
      throw new IllegalArgumentException("Only owner can demote admins");
    }

    GroupMember member =
        memberRepository
            .findByGroupIdAndUserId(groupId, userId)
            .orElseThrow(() -> new IllegalArgumentException("User not in group"));

    if (member.getRole() != GroupRole.ADMIN) {
      throw new IllegalArgumentException("Only admins can be demoted");
    }

    member.setRole(GroupRole.MEMBER);
    memberRepository.save(member);
  }

  public void removeMember(UUID groupId, UUID userId, AppUser requester) {
    Group group =
        groupRepository
            .findById(groupId)
            .orElseThrow(() -> new IllegalArgumentException("Group not found"));

    if (!permissionService.isAdmin(group, requester)) {
      throw new IllegalArgumentException("Only admins can remove members");
    }

    GroupMember member =
        memberRepository
            .findByGroupIdAndUserId(groupId, userId)
            .orElseThrow(() -> new IllegalArgumentException("User not in group"));

    if (member.getRole() == GroupRole.OWNER) {
      throw new IllegalArgumentException("Owner cannot be removed");
    }

    if (member.getRole() == GroupRole.ADMIN && !permissionService.isOwner(group, requester)) {
      throw new IllegalArgumentException("Only owner can remove admins");
    }

    memberRepository.delete(member);
  }

  @Transactional
  public void transferOwnership(UUID groupId, UUID newOwnerId, AppUser requester) {
    Group group =
        groupRepository
            .findById(groupId)
            .orElseThrow(() -> new IllegalArgumentException("Group not found"));

    if (!permissionService.isOwner(group, requester)) {
      throw new IllegalArgumentException("Only owner can transfer ownership");
    }

    if (requester.getId().equals(newOwnerId)) {
      throw new IllegalArgumentException("New owner must be another group member");
    }

    GroupMember newOwner =
        memberRepository
            .findByGroupIdAndUserId(groupId, newOwnerId)
            .orElseThrow(() -> new IllegalArgumentException("User not in group"));

    if (newOwner.getUser().getDeletedAt() != null) {
      throw new IllegalArgumentException("New owner account is not active");
    }
    subscriptionLimitService.assertCanCreateGroup(newOwner.getUser());

    // demote old owner
    GroupMember oldOwner =
        memberRepository.findByGroupIdAndUserId(group.getId(), requester.getId()).orElseThrow();

    oldOwner.setRole(GroupRole.ADMIN);
    newOwner.setRole(GroupRole.OWNER);

    memberRepository.save(oldOwner);
    memberRepository.save(newOwner);
  }
}
