package com.skoryk.projects.meetler.group.member;

import com.skoryk.projects.meetler.group.Group;
import com.skoryk.projects.meetler.group.GroupRepository;
import com.skoryk.projects.meetler.user.AppUser;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Groups")
@RestController
@RequestMapping("/api/groups/{groupId}/admin")
@RequiredArgsConstructor
public class GroupAdminController {

  private final GroupRepository groupRepository;
  private final GroupMemberRepository memberRepository;
  private final GroupPermissionService permissionService;

  @PostMapping("/promote/{userId}")
  public ResponseEntity<Void> promoteToAdmin(
      @PathVariable UUID groupId,
      @PathVariable UUID userId,
      @AuthenticationPrincipal AppUser requester) {
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

    return ResponseEntity.ok().build();
  }

  @PostMapping("/demote/{userId}")
  public ResponseEntity<Void> demoteAdmin(
      @PathVariable UUID groupId,
      @PathVariable UUID userId,
      @AuthenticationPrincipal AppUser requester) {
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

    return ResponseEntity.ok().build();
  }

  @DeleteMapping("/remove/{userId}")
  public ResponseEntity<Void> removeMember(
      @PathVariable UUID groupId,
      @PathVariable UUID userId,
      @AuthenticationPrincipal AppUser requester) {
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

    return ResponseEntity.noContent().build();
  }

  @Transactional
  @PostMapping("/transfer/{newOwnerId}")
  public ResponseEntity<Void> transferOwnership(
      @PathVariable UUID groupId,
      @PathVariable UUID newOwnerId,
      @AuthenticationPrincipal AppUser requester) {
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

    // demote old owner
    GroupMember oldOwner =
        memberRepository.findByGroupIdAndUserId(group.getId(), requester.getId()).orElseThrow();

    oldOwner.setRole(GroupRole.ADMIN);
    newOwner.setRole(GroupRole.OWNER);

    memberRepository.save(oldOwner);
    memberRepository.save(newOwner);

    return ResponseEntity.ok().build();
  }
}
