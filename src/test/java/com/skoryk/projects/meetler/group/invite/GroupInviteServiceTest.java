package com.skoryk.projects.meetler.group.invite;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.skoryk.projects.meetler.group.Group;
import com.skoryk.projects.meetler.group.GroupRepository;
import com.skoryk.projects.meetler.group.invite.dto.GroupInviteResponse;
import com.skoryk.projects.meetler.group.member.GroupMemberService;
import com.skoryk.projects.meetler.group.member.GroupPermissionService;
import com.skoryk.projects.meetler.group.member.GroupRole;
import com.skoryk.projects.meetler.user.AppUser;
import com.skoryk.projects.meetler.user.AppUserRole;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GroupInviteServiceTest {

  @Mock private GroupRepository groupRepository;
  @Mock private GroupInviteRepository inviteRepository;
  @Mock private GroupMemberService memberService;
  @Mock private GroupPermissionService groupPermissionService;
  @Spy private GroupInviteMapper groupInviteMapper = Mappers.getMapper(GroupInviteMapper.class);

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
  void createInviteReturnsCompletePersistedInvite() {
    Group group = group();
    AppUser user = user();
    UUID inviteId = UUID.randomUUID();
    OffsetDateTime expiresAt = OffsetDateTime.now().plusDays(7);
    when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));
    when(groupPermissionService.isAdmin(group, user)).thenReturn(true);
    doAnswer(
            invocation -> {
              GroupInvite invite = invocation.getArgument(0);
              invite.setId(inviteId);
              return invite;
            })
        .when(inviteRepository)
        .save(any(GroupInvite.class));

    GroupInviteResponse response = service.createInvite(group.getId(), user, 10, expiresAt);

    assertThat(response.getId()).isEqualTo(inviteId);
    assertThat(response.getCode()).hasSize(8);
    assertThat(response.getMaxUses()).isEqualTo(10);
    assertThat(response.getUses()).isZero();
    assertThat(response.getExpiresAt()).isEqualTo(expiresAt);
    assertThat(response.getCreatedAt()).isNotNull();
    assertThat(response.getRevokedAt()).isNull();
    assertThat(response.getRevokedByUserId()).isNull();
  }

  @Test
  void listActiveInvitesRequiresAdminPermission() {
    Group group = group();
    AppUser user = user();
    when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));
    when(groupPermissionService.isAdmin(group, user)).thenReturn(false);

    assertThatThrownBy(() -> service.listActiveInvites(group.getId(), user))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("User not allowed to view invites");
  }

  @Test
  void listActiveInvitesReturnsOnlyNotExpiredInvitesWithUsesLeft() {
    Group group = group();
    AppUser user = user();
    GroupInvite activeUnlimited = invite(group);
    activeUnlimited.setCode("active1");
    GroupInvite activeLimited = invite(group);
    activeLimited.setCode("active2");
    activeLimited.setMaxUses(3);
    activeLimited.setUses(2);
    GroupInvite expired = invite(group);
    expired.setCode("expired");
    expired.setExpiresAt(OffsetDateTime.now().minusMinutes(1));
    GroupInvite maxedOut = invite(group);
    maxedOut.setCode("maxed");
    maxedOut.setMaxUses(2);
    maxedOut.setUses(2);
    GroupInvite revoked = invite(group);
    revoked.setCode("revoked");
    revoked.setRevokedAt(OffsetDateTime.now().minusMinutes(1));
    revoked.setRevokedBy(user());

    when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));
    when(groupPermissionService.isAdmin(group, user)).thenReturn(true);
    when(inviteRepository.findByGroupOrderByCreatedAtDesc(group))
        .thenReturn(List.of(activeUnlimited, activeLimited, expired, maxedOut, revoked));

    List<GroupInviteResponse> invites = service.listActiveInvites(group.getId(), user);

    assertThat(invites)
        .extracting(GroupInviteResponse::getCode)
        .containsExactly("active1", "active2");
  }

  @Test
  void revokeInviteRequiresAdminPermission() {
    Group group = group();
    GroupInvite invite = invite(group);
    AppUser user = user();
    when(inviteRepository.findById(invite.getId())).thenReturn(Optional.of(invite));
    when(groupPermissionService.isAdmin(group, user)).thenReturn(false);

    assertThatThrownBy(() -> service.revokeInvite(invite.getId(), user))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("User not allowed to revoke invites");

    assertThat(invite.getRevokedAt()).isNull();
    verify(inviteRepository, never()).save(any());
  }

  @Test
  void revokeInviteStoresRevocationMetadata() {
    Group group = group();
    GroupInvite invite = invite(group);
    AppUser user = user();
    when(inviteRepository.findById(invite.getId())).thenReturn(Optional.of(invite));
    when(groupPermissionService.isAdmin(group, user)).thenReturn(true);

    GroupInviteResponse response = service.revokeInvite(invite.getId(), user);

    assertThat(invite.getRevokedAt()).isNotNull();
    assertThat(invite.getRevokedBy()).isEqualTo(user);
    assertThat(response.getRevokedAt()).isNotNull();
    assertThat(response.getRevokedByUserId()).isEqualTo(user.getId());
    verify(inviteRepository).save(invite);
  }

  @Test
  void joinWithCodeRejectsRevokedInvite() {
    GroupInvite invite = invite(group());
    invite.setRevokedAt(OffsetDateTime.now().minusMinutes(1));
    invite.setRevokedBy(user());
    when(inviteRepository.findByCode("abc")).thenReturn(Optional.of(invite));

    assertThatThrownBy(() -> service.joinWithCode("abc", user()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Invite revoked");
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
