package com.skoryk.projects.meetler.group;

import com.skoryk.projects.meetler.group.dto.CreateGroupRequest;
import com.skoryk.projects.meetler.group.dto.GroupResponse;
import com.skoryk.projects.meetler.group.dto.UpdateGroupRequest;
import com.skoryk.projects.meetler.user.AppUser;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class GroupController implements GroupApi {

  private final GroupService groupService;

  @Override
  public ResponseEntity<GroupResponse> createGroup(CreateGroupRequest request, AppUser user) {
    return ResponseEntity.ok(groupService.createGroup(request, user));
  }

  @Override
  public ResponseEntity<GroupResponse> updateGroup(
      UUID groupId, UpdateGroupRequest request, AppUser user) {
    return ResponseEntity.ok(groupService.updateGroup(groupId, request, user));
  }

  @Override
  public ResponseEntity<Void> deleteGroup(UUID groupId, AppUser user) {
    groupService.deleteGroup(groupId, user);
    return ResponseEntity.noContent().build();
  }
}
