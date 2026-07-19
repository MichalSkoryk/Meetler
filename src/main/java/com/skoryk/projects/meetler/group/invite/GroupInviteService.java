package com.skoryk.projects.meetler.group.invite;

import com.skoryk.projects.meetler.group.Group;
import com.skoryk.projects.meetler.group.GroupRepository;
import com.skoryk.projects.meetler.group.invite.dto.GroupInviteResponse;
import com.skoryk.projects.meetler.group.member.GroupMemberService;
import com.skoryk.projects.meetler.group.member.GroupPermissionService;
import com.skoryk.projects.meetler.group.member.GroupRole;
import com.skoryk.projects.meetler.user.AppUser;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GroupInviteService {

  private final GroupRepository groupRepository;
  private final GroupInviteRepository inviteRepository;
  private final GroupMemberService memberService;
  private final GroupPermissionService groupPermissionService;

  public String createInvite(
      UUID groupId, AppUser user, Integer maxUses, OffsetDateTime expiresAt) {
    Group group =
        groupRepository
            .findById(groupId)
            .orElseThrow(() -> new IllegalArgumentException("Group not found"));

    if (!groupPermissionService.isAdmin(group, user)) {
      throw new IllegalArgumentException("User not allowed to create invites");
    }

    String code = UUID.randomUUID().toString().replace("-", "").substring(0, 8);

    GroupInvite invite =
        GroupInvite.builder()
            .group(group)
            .code(code)
            .maxUses(maxUses)
            .expiresAt(expiresAt)
            .uses(0)
            .createdAt(OffsetDateTime.now())
            .build();

    inviteRepository.save(invite);

    return code;
  }

  @Transactional(readOnly = true)
  public List<GroupInviteResponse> listActiveInvites(UUID groupId, AppUser user) {
    Group group =
        groupRepository
            .findById(groupId)
            .orElseThrow(() -> new IllegalArgumentException("Group not found"));

    if (!groupPermissionService.isAdmin(group, user)) {
      throw new IllegalArgumentException("User not allowed to view invites");
    }

    OffsetDateTime now = OffsetDateTime.now();
    return inviteRepository.findByGroupOrderByCreatedAtDesc(group).stream()
        .filter(invite -> isActive(invite, now))
        .map(this::toResponse)
        .toList();
  }

  @Transactional
  public GroupInviteResponse revokeInvite(UUID inviteId, AppUser user) {
    GroupInvite invite =
        inviteRepository
            .findById(inviteId)
            .orElseThrow(() -> new IllegalArgumentException("Invite not found"));

    if (!groupPermissionService.isAdmin(invite.getGroup(), user)) {
      throw new IllegalArgumentException("User not allowed to revoke invites");
    }

    if (invite.getRevokedAt() == null) {
      invite.setRevokedAt(OffsetDateTime.now());
      invite.setRevokedBy(user);
      inviteRepository.save(invite);
    }

    return toResponse(invite);
  }

  @Transactional
  public void joinWithCode(String code, AppUser user) {
    GroupInvite invite =
        inviteRepository
            .findByCode(code)
            .orElseThrow(() -> new IllegalArgumentException("Invalid invite code"));

    joinWithInvite(invite, user);
  }

  @Transactional
  public void joinGroupWithCode(UUID groupId, String code, AppUser user) {
    GroupInvite invite =
        inviteRepository
            .findByCode(code)
            .orElseThrow(() -> new IllegalArgumentException("Invalid invite code"));

    if (!invite.getGroup().getId().equals(groupId)) {
      throw new IllegalArgumentException("Invalid invite code");
    }

    joinWithInvite(invite, user);
  }

  private void joinWithInvite(GroupInvite invite, AppUser user) {
    if (invite.getRevokedAt() != null) {
      throw new IllegalArgumentException("Invite revoked");
    }

    if (invite.getExpiresAt() != null && invite.getExpiresAt().isBefore(OffsetDateTime.now())) {
      throw new IllegalArgumentException("Invite expired");
    }

    if (invite.getMaxUses() != null && invite.getUses() >= invite.getMaxUses()) {
      throw new IllegalArgumentException("Invite limit reached");
    }

    invite.setUses(invite.getUses() + 1);
    inviteRepository.save(invite);

    memberService.addMember(invite.getGroup().getId(), user, GroupRole.MEMBER);
  }

  private boolean isActive(GroupInvite invite, OffsetDateTime now) {
    boolean notRevoked = invite.getRevokedAt() == null;
    boolean notExpired = invite.getExpiresAt() == null || invite.getExpiresAt().isAfter(now);
    boolean hasUsesLeft = invite.getMaxUses() == null || invite.getUses() < invite.getMaxUses();
    return notRevoked && notExpired && hasUsesLeft;
  }

  private GroupInviteResponse toResponse(GroupInvite invite) {
    return GroupInviteResponse.builder()
        .id(invite.getId())
        .code(invite.getCode())
        .expiresAt(invite.getExpiresAt())
        .maxUses(invite.getMaxUses())
        .uses(invite.getUses())
        .createdAt(invite.getCreatedAt())
        .revokedAt(invite.getRevokedAt())
        .revokedByUserId(invite.getRevokedBy() == null ? null : invite.getRevokedBy().getId())
        .build();
  }
}
