package com.skoryk.projects.meetler.group.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.skoryk.projects.meetler.group.Group;
import com.skoryk.projects.meetler.group.GroupRepository;
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
class GroupAdminControllerTest {

  @Mock private GroupRepository groupRepository;
  @Mock private GroupMemberRepository memberRepository;
  @Mock private GroupPermissionService permissionService;

  @InjectMocks private GroupAdminController controller;

  @Test
  void promoteRequiresOwner() {
    Group group = group();
    AppUser requester = user();
    when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));
    when(permissionService.isOwner(group, requester)).thenReturn(false);

    assertThatThrownBy(() -> controller.promoteToAdmin(group.getId(), UUID.randomUUID(), requester))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Only owner can promote admins");

    verify(memberRepository, never()).save(any());
  }

  @Test
  void promoteChangesMemberRoleToAdmin() {
    Group group = group();
    AppUser requester = user();
    AppUser target = user();
    GroupMember member = member(group, target, GroupRole.MEMBER);
    when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));
    when(permissionService.isOwner(group, requester)).thenReturn(true);
    when(memberRepository.findByGroupIdAndUserId(group.getId(), target.getId()))
        .thenReturn(Optional.of(member));

    controller.promoteToAdmin(group.getId(), target.getId(), requester);

    assertThat(member.getRole()).isEqualTo(GroupRole.ADMIN);
    verify(memberRepository).save(member);
  }

  @Test
  void removeAdminRequiresOwner() {
    Group group = group();
    AppUser requester = user();
    AppUser target = user();
    GroupMember member = member(group, target, GroupRole.ADMIN);
    when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));
    when(permissionService.isAdmin(group, requester)).thenReturn(true);
    when(permissionService.isOwner(group, requester)).thenReturn(false);
    when(memberRepository.findByGroupIdAndUserId(group.getId(), target.getId()))
        .thenReturn(Optional.of(member));

    assertThatThrownBy(() -> controller.removeMember(group.getId(), target.getId(), requester))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Only owner can remove admins");

    verify(memberRepository, never()).delete(any());
  }

  @Test
  void transferOwnershipDemotesOldOwnerAndPromotesNewOwner() {
    Group group = group();
    AppUser oldOwner = user();
    AppUser newOwnerUser = user();
    GroupMember oldOwnerMember = member(group, oldOwner, GroupRole.OWNER);
    GroupMember newOwnerMember = member(group, newOwnerUser, GroupRole.ADMIN);
    when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));
    when(permissionService.isOwner(group, oldOwner)).thenReturn(true);
    when(memberRepository.findByGroupIdAndUserId(group.getId(), newOwnerUser.getId()))
        .thenReturn(Optional.of(newOwnerMember));
    when(memberRepository.findByGroupIdAndUserId(group.getId(), oldOwner.getId()))
        .thenReturn(Optional.of(oldOwnerMember));

    controller.transferOwnership(group.getId(), newOwnerUser.getId(), oldOwner);

    assertThat(oldOwnerMember.getRole()).isEqualTo(GroupRole.ADMIN);
    assertThat(newOwnerMember.getRole()).isEqualTo(GroupRole.OWNER);
    verify(memberRepository).save(oldOwnerMember);
    verify(memberRepository).save(newOwnerMember);
  }

  private GroupMember member(Group group, AppUser user, GroupRole role) {
    return GroupMember.builder()
        .id(UUID.randomUUID())
        .group(group)
        .user(user)
        .role(role)
        .joinedAt(OffsetDateTime.now())
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
        .email(UUID.randomUUID() + "@example.com")
        .role(AppUserRole.USER)
        .build();
  }
}
