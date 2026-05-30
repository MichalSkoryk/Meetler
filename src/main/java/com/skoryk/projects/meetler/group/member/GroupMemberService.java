package com.skoryk.projects.meetler.group.member;

import com.skoryk.projects.meetler.group.Group;
import com.skoryk.projects.meetler.group.GroupRepository;
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

  public void addMember(UUID groupId, AppUser user, GroupRole role) {
    Group group =
        groupRepository
            .findById(groupId)
            .orElseThrow(() -> new IllegalArgumentException("Group not found"));

    if (memberRepository.findByGroupAndUser(group, user).isPresent()) {
      return; // already a member
    }

    GroupMember member =
        GroupMember.builder()
            .group(group)
            .user(user)
            .role(role)
            .joinedAt(OffsetDateTime.now())
            .build();

    memberRepository.save(member);
  }

  @Transactional
  public void removeMember(UUID groupId, AppUser user) {
    Group group =
        groupRepository
            .findById(groupId)
            .orElseThrow(() -> new IllegalArgumentException("Group not found"));

    memberRepository.deleteByGroupIdAndUserId(groupId, user.getId());
  }

  public List<GroupMember> getGroupMembers(UUID groupId) {
    Group group =
        groupRepository
            .findById(groupId)
            .orElseThrow(() -> new IllegalArgumentException("Group not found"));

    return memberRepository.findByGroup(group);
  }
}
