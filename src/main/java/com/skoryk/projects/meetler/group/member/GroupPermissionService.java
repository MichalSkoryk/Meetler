package com.skoryk.projects.meetler.group.member;

import com.skoryk.projects.meetler.group.Group;
import com.skoryk.projects.meetler.user.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GroupPermissionService {

  private final GroupMemberRepository memberRepository;

  public GroupRole getUserRole(Group group, AppUser user) {
    return memberRepository.findByGroupAndUser(group, user).map(GroupMember::getRole).orElse(null);
  }

  public boolean isOwner(Group group, AppUser user) {
    return getUserRole(group, user) == GroupRole.OWNER;
  }

  public boolean isAdmin(Group group, AppUser user) {
    GroupRole role = getUserRole(group, user);
    return role == GroupRole.ADMIN || role == GroupRole.OWNER;
  }

  public boolean isMember(Group group, AppUser user) {
    return getUserRole(group, user) != null;
  }
}
