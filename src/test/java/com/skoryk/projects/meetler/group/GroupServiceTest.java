package com.skoryk.projects.meetler.group;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.skoryk.projects.meetler.group.dto.CreateGroupRequest;
import com.skoryk.projects.meetler.group.dto.GroupResponse;
import com.skoryk.projects.meetler.group.dto.UpdateGroupRequest;
import com.skoryk.projects.meetler.group.event.GroupEvent;
import com.skoryk.projects.meetler.group.event.GroupEventParticipant;
import com.skoryk.projects.meetler.group.event.GroupEventParticipantRepository;
import com.skoryk.projects.meetler.group.event.GroupEventParticipantStatus;
import com.skoryk.projects.meetler.group.event.GroupEventRepository;
import com.skoryk.projects.meetler.group.event.GroupEventStatus;
import com.skoryk.projects.meetler.group.member.GroupMember;
import com.skoryk.projects.meetler.group.member.GroupMemberRepository;
import com.skoryk.projects.meetler.group.member.GroupMemberService;
import com.skoryk.projects.meetler.group.member.GroupPermissionService;
import com.skoryk.projects.meetler.group.member.GroupRole;
import com.skoryk.projects.meetler.subscription.SubscriptionLimitService;
import com.skoryk.projects.meetler.user.AppUser;
import com.skoryk.projects.meetler.user.AppUserRole;
import java.time.OffsetDateTime;
import java.util.List;
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
  @Mock private GroupEventRepository groupEventRepository;
  @Mock private GroupEventParticipantRepository groupEventParticipantRepository;
  @Mock private SubscriptionLimitService subscriptionLimitService;

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
    when(groupMemberService.addMember(any(UUID.class), eq(owner), eq(GroupRole.OWNER)))
        .thenAnswer(
            invocation ->
                GroupMember.builder()
                    .group(
                        Group.builder()
                            .id(invocation.getArgument(0))
                            .name("Friends")
                            .createdAt(OffsetDateTime.now())
                            .updatedAt(OffsetDateTime.now())
                            .build())
                    .user(owner)
                    .role(GroupRole.OWNER)
                    .build());

    GroupResponse response = service.createGroup(request, owner);

    assertThat(response.getName()).isEqualTo("Friends");
    assertThat(response.getRole()).isEqualTo("OWNER");
    verify(subscriptionLimitService).assertCanCreateGroup(owner);
    verify(groupMemberService).addMember(response.getId(), owner, GroupRole.OWNER);
  }

  @Test
  void getMyGroupsReturnsBatchedDashboardSummary() {
    AppUser user = user();
    Group group = group();
    GroupMember membership =
        GroupMember.builder().group(group).user(user).role(GroupRole.OWNER).build();
    GroupMember secondMember =
        GroupMember.builder()
            .group(group)
            .user(AppUser.builder().id(UUID.randomUUID()).build())
            .role(GroupRole.MEMBER)
            .build();
    GroupEvent pendingEvent =
        GroupEvent.builder()
            .id(UUID.randomUUID())
            .group(group)
            .createdBy(user)
            .title("Board games")
            .startsAt(OffsetDateTime.now().plusDays(1))
            .endsAt(OffsetDateTime.now().plusDays(1).plusHours(2))
            .status(GroupEventStatus.PENDING_CONFIRMATION)
            .requiresConfirmation(true)
            .build();
    GroupEventParticipant participant =
        GroupEventParticipant.builder()
            .groupEvent(pendingEvent)
            .user(user)
            .status(GroupEventParticipantStatus.PENDING)
            .build();

    when(groupMemberRepository.findByUser(user)).thenReturn(List.of(membership));
    when(groupMemberRepository.findByGroupIn(List.of(group)))
        .thenReturn(List.of(membership, secondMember));
    when(groupEventRepository.findUpcomingByGroups(
            eq(List.of(group)), any(OffsetDateTime.class), eq(GroupEventStatus.CANCELLED)))
        .thenReturn(List.of(pendingEvent));
    when(groupEventParticipantRepository.findByGroupEventInAndUser(List.of(pendingEvent), user))
        .thenReturn(List.of(participant));

    GroupResponse response = service.getMyGroups(user).getFirst();

    assertThat(response.getMemberCount()).isEqualTo(2);
    assertThat(response.isHasAvailabilityTemplate()).isFalse();
    assertThat(response.getPendingResponseCount()).isEqualTo(1);
    assertThat(response.getNextEvent().getTitle()).isEqualTo("Board games");
    assertThat(response.getNextPendingEvent().getMyResponseStatus()).isEqualTo("PENDING");
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
