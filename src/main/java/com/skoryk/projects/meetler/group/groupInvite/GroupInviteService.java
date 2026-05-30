package com.skoryk.projects.meetler.group.groupInvite;

import com.skoryk.projects.meetler.group.Group;
import com.skoryk.projects.meetler.group.GroupRepository;
import com.skoryk.projects.meetler.group.member.GroupMemberService;
import com.skoryk.projects.meetler.group.member.GroupRole;
import com.skoryk.projects.meetler.user.AppUser;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GroupInviteService {

  private final GroupRepository groupRepository;
  private final GroupInviteRepository inviteRepository;
  private final GroupMemberService memberService;

  public String createInvite(UUID groupId, Integer maxUses, OffsetDateTime expiresAt) {
    Group group =
        groupRepository
            .findById(groupId)
            .orElseThrow(() -> new IllegalArgumentException("Group not found"));

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

  public void joinWithCode(String code, AppUser user) {
    GroupInvite invite =
        inviteRepository
            .findByCode(code)
            .orElseThrow(() -> new IllegalArgumentException("Invalid invite code"));

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
}
