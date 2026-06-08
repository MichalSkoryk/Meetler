package com.skoryk.projects.meetler.group;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.skoryk.projects.meetler.availability.dto.ResolvedAvailabilityWindowResponse;
import com.skoryk.projects.meetler.availability.model.AvailabilityBlockStatus;
import com.skoryk.projects.meetler.availability.model.AvailabilityTemplate;
import com.skoryk.projects.meetler.availability.model.ResolvedAvailabilitySource;
import com.skoryk.projects.meetler.availability.resolver.AvailabilityTemplateResolver;
import com.skoryk.projects.meetler.group.dto.GroupAvailabilitySlotResponse;
import com.skoryk.projects.meetler.group.member.GroupMember;
import com.skoryk.projects.meetler.group.member.GroupMemberRepository;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GroupAvailabilityServiceTest {

  @Mock private GroupRepository groupRepository;
  @Mock private GroupMemberRepository memberRepository;
  @Mock private GroupPermissionService permissionService;
  @Mock private AvailabilityTemplateResolver availabilityTemplateResolver;

  @InjectMocks private GroupAvailabilityService service;

  private final OffsetDateTime from = OffsetDateTime.parse("2026-06-08T10:00:00+02:00");
  private final OffsetDateTime to = OffsetDateTime.parse("2026-06-08T10:30:00+02:00");

  @Test
  void gridRequiresRequesterToBeGroupMember() {
    Group group = group();
    AppUser requester = user();
    when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));
    when(permissionService.isMember(group, requester)).thenReturn(false);

    assertThatThrownBy(() -> service.getAvailabilityGrid(group.getId(), requester, from, to))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Not allowed");
  }

  @Test
  void gridCountsAvailableBusyAndNoTemplateMembers() {
    Group group = group();
    AppUser requester = user();
    AppUser availableUser = user();
    AppUser busyUser = user();
    AppUser noTemplateUser = user();
    AvailabilityTemplate availableTemplate = template(availableUser);
    AvailabilityTemplate busyTemplate = template(busyUser);
    GroupMember availableMember = member(group, availableUser, availableTemplate);
    GroupMember busyMember = member(group, busyUser, busyTemplate);
    GroupMember noTemplateMember = member(group, noTemplateUser, null);

    when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));
    when(permissionService.isMember(group, requester)).thenReturn(true);
    when(memberRepository.findByGroup(group))
        .thenReturn(List.of(availableMember, busyMember, noTemplateMember));
    when(availabilityTemplateResolver.resolve(availableTemplate, from, to))
        .thenReturn(List.of(window(from, to, AvailabilityBlockStatus.AVAILABLE)));
    when(availabilityTemplateResolver.resolve(busyTemplate, from, to))
        .thenReturn(
            List.of(
                window(
                    OffsetDateTime.parse("2026-06-08T10:10:00+02:00"),
                    OffsetDateTime.parse("2026-06-08T10:20:00+02:00"),
                    AvailabilityBlockStatus.BUSY)));

    List<GroupAvailabilitySlotResponse> slots =
        service.getAvailabilityGrid(group.getId(), requester, from, to);

    assertThat(slots).hasSize(2);
    assertThat(slots.getFirst().getAvailableUserIds()).containsExactly(availableUser.getId());
    assertThat(slots.getFirst().getBusyUserIds()).containsExactly(busyUser.getId());
    assertThat(slots.getFirst().getNoTemplateUserIds()).containsExactly(noTemplateUser.getId());
  }

  @Test
  void gridRejectsRangeLongerThanThirtyOneDays() {
    assertThatThrownBy(
            () -> service.getAvailabilityGrid(UUID.randomUUID(), user(), from, from.plusDays(32)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Availability grid range must not be longer than 31 days");
  }

  private ResolvedAvailabilityWindowResponse window(
      OffsetDateTime startsAt, OffsetDateTime endsAt, AvailabilityBlockStatus status) {
    return ResolvedAvailabilityWindowResponse.builder()
        .startsAt(startsAt)
        .endsAt(endsAt)
        .status(status)
        .source(ResolvedAvailabilitySource.DEFAULT)
        .build();
  }

  private GroupMember member(Group group, AppUser user, AvailabilityTemplate template) {
    return GroupMember.builder()
        .id(UUID.randomUUID())
        .group(group)
        .user(user)
        .role(GroupRole.MEMBER)
        .availabilityTemplate(template)
        .joinedAt(OffsetDateTime.now())
        .build();
  }

  private AvailabilityTemplate template(AppUser user) {
    return AvailabilityTemplate.builder()
        .id(UUID.randomUUID())
        .user(user)
        .name("Template")
        .timezone("Europe/Warsaw")
        .defaultAvailabilityStatus(AvailabilityBlockStatus.BUSY)
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
