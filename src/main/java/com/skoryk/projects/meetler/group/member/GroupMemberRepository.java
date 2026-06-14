package com.skoryk.projects.meetler.group.member;

import com.skoryk.projects.meetler.group.Group;
import com.skoryk.projects.meetler.user.AppUser;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupMemberRepository extends JpaRepository<GroupMember, UUID> {

  List<GroupMember> findByUser(AppUser user);

  long countByUserAndRole(AppUser user, GroupRole role);

  List<GroupMember> findByGroup(Group group);

  Optional<GroupMember> findByGroupIdAndUserId(UUID groupId, UUID userId);

  Optional<GroupMember> findByGroupAndUser(Group group, AppUser user);

  void deleteByGroupAndUser(Group group, AppUser user);

  void deleteByGroupIdAndUserId(UUID groupId, UUID userId);
}
