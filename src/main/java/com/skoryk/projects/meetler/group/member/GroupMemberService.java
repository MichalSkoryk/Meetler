package com.skoryk.projects.meetler.group.member;

import com.skoryk.projects.meetler.availability.model.AvailabilityTemplate;
import com.skoryk.projects.meetler.availability.repository.AvailabilityTemplateRepository;
import com.skoryk.projects.meetler.group.Group;
import com.skoryk.projects.meetler.group.GroupRepository;
import com.skoryk.projects.meetler.group.member.dto.GroupAvailabilityTemplateResponse;
import com.skoryk.projects.meetler.group.member.dto.GroupMemberResponse;
import com.skoryk.projects.meetler.user.AppUser;
import jakarta.transaction.Transactional;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GroupMemberService {

  private final GroupRepository groupRepository;
  private final GroupMemberRepository memberRepository;
  private final AvailabilityTemplateRepository availabilityTemplateRepository;
  private final GroupMemberMapper groupMemberMapper;

  public GroupMember addMember(UUID groupId, AppUser user, GroupRole role) {
    Group group =
        groupRepository
            .findById(groupId)
            .orElseThrow(() -> new IllegalArgumentException("Group not found"));

    GroupMember existingMember = memberRepository.findByGroupAndUser(group, user).orElse(null);
    if (existingMember != null) {
      return existingMember;
    }

    AvailabilityTemplate defaultTemplate =
        availabilityTemplateRepository.findByUserAndIsDefaultTrue(user).orElse(null);

    GroupMember member =
        GroupMember.builder()
            .group(group)
            .user(user)
            .role(role)
            .availabilityTemplate(defaultTemplate)
            .joinedAt(OffsetDateTime.now())
            .build();

    return memberRepository.save(member);
  }

  @Transactional
  public void removeMember(UUID groupId, AppUser user) {
    Group group =
        groupRepository
            .findById(groupId)
            .orElseThrow(() -> new IllegalArgumentException("Group not found"));

    GroupMember member =
        memberRepository
            .findByGroupAndUser(group, user)
            .orElseThrow(() -> new IllegalArgumentException("User not in group"));

    if (member.getRole() == GroupRole.OWNER) {
      throw new IllegalArgumentException("Owner must transfer ownership before leaving");
    }

    memberRepository.delete(member);
  }

  @Transactional
  public List<GroupMemberResponse> getGroupMembers(UUID groupId) {
    Group group =
        groupRepository
            .findById(groupId)
            .orElseThrow(() -> new IllegalArgumentException("Group not found"));

    return memberRepository.findByGroup(group).stream()
        .map(groupMemberMapper::toMemberResponse)
        .toList();
  }

  @Transactional
  public GroupAvailabilityTemplateResponse selectAvailabilityTemplate(
      UUID groupId, AppUser user, UUID availabilityTemplateId) {
    GroupMember member = getCurrentMember(groupId, user);
    AvailabilityTemplate template =
        availabilityTemplateRepository
            .findByIdAndUser(availabilityTemplateId, user)
            .orElseThrow(() -> new IllegalArgumentException("Availability template not found"));

    member.setAvailabilityTemplate(template);
    return groupMemberMapper.toAvailabilityTemplateResponse(memberRepository.save(member));
  }

  @Transactional
  public GroupAvailabilityTemplateResponse clearAvailabilityTemplate(UUID groupId, AppUser user) {
    GroupMember member = getCurrentMember(groupId, user);
    member.setAvailabilityTemplate(null);
    return groupMemberMapper.toAvailabilityTemplateResponse(memberRepository.save(member));
  }

  public GroupAvailabilityTemplateResponse getAvailabilityTemplate(UUID groupId, AppUser user) {
    return groupMemberMapper.toAvailabilityTemplateResponse(getCurrentMember(groupId, user));
  }

  private GroupMember getCurrentMember(UUID groupId, AppUser user) {
    return memberRepository
        .findByGroupIdAndUserId(groupId, user.getId())
        .orElseThrow(() -> new IllegalArgumentException("User not in group"));
  }
}
