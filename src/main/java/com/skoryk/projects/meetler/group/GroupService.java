package com.skoryk.projects.meetler.group;

import com.skoryk.projects.meetler.group.dto.CreateGroupRequest;
import com.skoryk.projects.meetler.group.dto.GroupResponse;
import com.skoryk.projects.meetler.group.dto.UpdateGroupRequest;
import com.skoryk.projects.meetler.group.member.*;
import com.skoryk.projects.meetler.user.AppUser;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GroupService {

  private final GroupRepository groupRepository;
  private final GroupPermissionService groupPermissionService;
  private final GroupMemberService groupMemberService;

  public GroupResponse createGroup(CreateGroupRequest request, AppUser owner) {
    // TODO: check user available groups

    Group group =
        Group.builder()
            .name(request.getName())
            .createdAt(OffsetDateTime.now())
            .updatedAt(OffsetDateTime.now())
            .build();

    Group savedGroup = groupRepository.save(group);

    groupMemberService.addMember(savedGroup.getId(), owner, GroupRole.OWNER);

    return toResponse(group);
  }

  public GroupResponse updateGroup(UUID groupId, UpdateGroupRequest request, AppUser user) {
    Group group =
        groupRepository
            .findById(groupId)
            .orElseThrow(() -> new IllegalArgumentException("Group not found"));

    if (!groupPermissionService.isMember(group, user)) {
      throw new IllegalArgumentException("Not allowed");
    }

    group.setName(request.getName());
    group.setUpdatedAt(OffsetDateTime.now());

    groupRepository.save(group);

    return toResponse(group);
  }

  public void deleteGroup(UUID groupId, AppUser user) {
    Group group =
        groupRepository
            .findById(groupId)
            .orElseThrow(() -> new IllegalArgumentException("Group not found"));

    if (groupPermissionService.isOwner(group, user)) {
      throw new IllegalArgumentException("Not allowed");
    }

    groupRepository.delete(group);
  }

  private GroupResponse toResponse(Group group) {
    return GroupResponse.builder()
        .id(group.getId())
        .name(group.getName())
        .createdAt(group.getCreatedAt())
        .updatedAt(group.getUpdatedAt())
        .build();
  }
}
