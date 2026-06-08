package com.skoryk.projects.meetler.group;

import com.skoryk.projects.meetler.group.dto.CreateGroupRequest;
import com.skoryk.projects.meetler.group.dto.GroupAvailabilitySlotResponse;
import com.skoryk.projects.meetler.group.dto.GroupResponse;
import com.skoryk.projects.meetler.group.dto.UpdateGroupRequest;
import com.skoryk.projects.meetler.user.AppUser;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class GroupController implements GroupApi {

  private final GroupService groupService;
  private final GroupAvailabilityService groupAvailabilityService;

  @Override
  public ResponseEntity<GroupResponse> createGroup(CreateGroupRequest request, AppUser user) {
    return ResponseEntity.ok(groupService.createGroup(request, user));
  }

  @Override
  public ResponseEntity<List<GroupResponse>> getMyGroups(AppUser user) {
    return ResponseEntity.ok(groupService.getMyGroups(user));
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

  @Override
  public ResponseEntity<List<GroupAvailabilitySlotResponse>> getAvailabilityGrid(
      UUID groupId, OffsetDateTime from, OffsetDateTime to, AppUser user) {
    return ResponseEntity.ok(groupAvailabilityService.getAvailabilityGrid(groupId, user, from, to));
  }
}
