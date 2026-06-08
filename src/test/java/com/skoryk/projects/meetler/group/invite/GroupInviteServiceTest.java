package com.skoryk.projects.meetler.group.invite;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.skoryk.projects.meetler.group.Group;
import com.skoryk.projects.meetler.group.GroupRepository;
import com.skoryk.projects.meetler.group.member.GroupMemberService;
import com.skoryk.projects.meetler.group.member.GroupPermissionService;
import com.skoryk.projects.meetler.group.member.GroupRole;
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
class GroupInviteServiceTest {

  @Mock private GroupRepository groupRepository;
  @Mock private GroupInviteRepository inviteRepository;
  @Mock private GroupMemberService memberService;
  @Mock private GroupPermissionService groupPermissionService;

  @InjectMocks private GroupInviteService service;

  @Test
  void createInviteRequiresAdminPermission() {
    Group group = group();
    AppUser user = user();
    when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));
    when(groupPermissionService.isAdmin(group, user)).thenReturn(false);

    assertThatThrownBy(() -> service.createInvite(group.getId(), user, 10, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("User not allowed to create invites");

    verify(inviteRepository, never()).save(any());
  }

  @Test
  void joinGroupWithCodeRejectsInviteForAnotherGroup() {
    Group group = group();
    GroupInvite invite = invite(group);
    UUID requestedGroupId = UUID.randomUUID();
    when(inviteRepository.findByCode("abc")).thenReturn(Optional.of(invite));

    assertThatThrownBy(() -> service.joinGroupWithCode(requestedGroupId, "abc", user()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Invalid invite code");
  }

  @Test
  void joinWithCodeRejectsExpiredInvite() {
    GroupInvite invite = invite(group());
    invite.setExpiresAt(OffsetDateTime.now().minusMinutes(1));
    when(inviteRepository.findByCode("abc")).thenReturn(Optional.of(invite));

    assertThatThrownBy(() -> service.joinWithCode("abc", user()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Invite expired");
  }

  @Test
  void joinWithCodeIncrementsUsesAndAddsMember() {
    Group group = group();
    AppUser user = user();
    GroupInvite invite = invite(group);
    invite.setMaxUses(2);
    invite.setUses(1);
    when(inviteRepository.findByCode("abc")).thenReturn(Optional.of(invite));

    service.joinWithCode("abc", user);

    assertThat(invite.getUses()).isEqualTo(2);
    verify(inviteRepository).save(invite);
    verify(memberService).addMember(group.getId(), user, GroupRole.MEMBER);
  }

  private GroupInvite invite(Group group) {
    return GroupInvite.builder()
        .id(UUID.randomUUID())
        .group(group)
        .code("abc")
        .uses(0)
        .createdAt(OffsetDateTime.now())
        .build();
  }

  private Group group() {
    return Group.builder()
        .id(UUID.randomUUID())
        .name("Group")
        .createdAt(OffsetDateTime.now())
        .updatedAt(OffsetDateTime.now())
        .build();
  }

  private AppUser user() {
    return AppUser.builder()
        .id(UUID.randomUUID())
        .email("test@example.com")
        .role(AppUserRole.USER)
        .build();
  }
}
