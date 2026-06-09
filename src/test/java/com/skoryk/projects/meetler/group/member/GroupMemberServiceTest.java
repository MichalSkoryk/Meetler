package com.skoryk.projects.meetler.group.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.skoryk.projects.meetler.availability.model.AvailabilityBlockStatus;
import com.skoryk.projects.meetler.availability.model.AvailabilityTemplate;
import com.skoryk.projects.meetler.availability.repository.AvailabilityTemplateRepository;
import com.skoryk.projects.meetler.group.Group;
import com.skoryk.projects.meetler.group.GroupRepository;
import com.skoryk.projects.meetler.group.member.dto.GroupAvailabilityTemplateResponse;
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
class GroupMemberServiceTest {

  @Mock private GroupRepository groupRepository;
  @Mock private GroupMemberRepository memberRepository;
  @Mock private AvailabilityTemplateRepository availabilityTemplateRepository;

  @InjectMocks private GroupMemberService service;

  @Test
  void addMemberDoesNothingWhenUserAlreadyMember() {
    Group group = group();
    AppUser user = user();
    when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));
    when(memberRepository.findByGroupAndUser(group, user))
        .thenReturn(Optional.of(member(group, user, GroupRole.MEMBER)));

    service.addMember(group.getId(), user, GroupRole.MEMBER);

    verify(memberRepository, never()).save(any());
  }

  @Test
  void removeMemberRejectsOwnerLeaving() {
    Group group = group();
    AppUser user = user();
    when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));
    when(memberRepository.findByGroupAndUser(group, user))
        .thenReturn(Optional.of(member(group, user, GroupRole.OWNER)));

    assertThatThrownBy(() -> service.removeMember(group.getId(), user))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Owner must transfer ownership before leaving");

    verify(memberRepository, never()).delete(any());
  }

  @Test
  void selectAvailabilityTemplateRequiresTemplateOwnedByUser() {
    Group group = group();
    AppUser user = user();
    GroupMember member = member(group, user, GroupRole.MEMBER);
    UUID templateId = UUID.randomUUID();
    when(memberRepository.findByGroupIdAndUserId(group.getId(), user.getId()))
        .thenReturn(Optional.of(member));
    when(availabilityTemplateRepository.findByIdAndUser(templateId, user))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.selectAvailabilityTemplate(group.getId(), user, templateId))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Availability template not found");
  }

  @Test
  void selectAvailabilityTemplateStoresTemplateOnMember() {
    Group group = group();
    AppUser user = user();
    GroupMember member = member(group, user, GroupRole.MEMBER);
    AvailabilityTemplate template = template(user);
    when(memberRepository.findByGroupIdAndUserId(group.getId(), user.getId()))
        .thenReturn(Optional.of(member));
    when(availabilityTemplateRepository.findByIdAndUser(template.getId(), user))
        .thenReturn(Optional.of(template));
    when(memberRepository.save(member)).thenReturn(member);

    GroupAvailabilityTemplateResponse response =
        service.selectAvailabilityTemplate(group.getId(), user, template.getId());

    assertThat(response.getAvailabilityTemplateId()).isEqualTo(template.getId());
    assertThat(member.getAvailabilityTemplate()).isSameAs(template);
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
        .email("test@example.com")
        .role(AppUserRole.USER)
        .build();
  }
}
