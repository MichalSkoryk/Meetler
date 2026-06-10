package com.skoryk.projects.meetler.group;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.skoryk.projects.meetler.group.dto.CreateGroupRequest;
import com.skoryk.projects.meetler.group.dto.GroupResponse;
import com.skoryk.projects.meetler.group.dto.UpdateGroupRequest;
import com.skoryk.projects.meetler.group.member.GroupMemberRepository;
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
class GroupServiceTest {

  @Mock private GroupRepository groupRepository;
  @Mock private GroupPermissionService groupPermissionService;
  @Mock private GroupMemberService groupMemberService;
  @Mock private GroupMemberRepository groupMemberRepository;

  @InjectMocks private GroupService service;

  @Test
  void createGroupAddsOwnerMembership() {
    AppUser owner = user();
    CreateGroupRequest request = new CreateGroupRequest();
    request.setName("Friends");
    when(groupRepository.save(any(Group.class)))
        .thenAnswer(
            invocation -> {
              Group group = invocation.getArgument(0);
              group.setId(UUID.randomUUID());
              return group;
            });

    GroupResponse response = service.createGroup(request, owner);

    assertThat(response.getName()).isEqualTo("Friends");
    assertThat(response.getRole()).isEqualTo("OWNER");
    verify(groupMemberService).addMember(response.getId(), owner, GroupRole.OWNER);
  }

  @Test
  void updateGroupRequiresAdmin() {
    AppUser user = user();
    Group group = group();
    UpdateGroupRequest request = new UpdateGroupRequest();
    request.setName("Updated");
    when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));
    when(groupPermissionService.isAdmin(group, user)).thenReturn(false);

    assertThatThrownBy(() -> service.updateGroup(group.getId(), request, user))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Not allowed");

    verify(groupRepository, never()).save(any());
  }

  @Test
  void deleteGroupRequiresOwner() {
    AppUser user = user();
    Group group = group();
    when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));
    when(groupPermissionService.isOwner(group, user)).thenReturn(false);

    assertThatThrownBy(() -> service.deleteGroup(group.getId(), user))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Not allowed");

    verify(groupRepository, never()).delete(any());
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
