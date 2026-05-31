package com.skoryk.projects.meetler.group;

import com.skoryk.projects.meetler.group.dto.CreateGroupRequest;
import com.skoryk.projects.meetler.group.dto.GroupResponse;
import com.skoryk.projects.meetler.group.dto.UpdateGroupRequest;
import com.skoryk.projects.meetler.user.AppUser;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Groups")
@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
public class GroupController {

  private final GroupService groupService;

  @PostMapping
  public ResponseEntity<GroupResponse> createGroup(
      @Valid @RequestBody CreateGroupRequest request, @AuthenticationPrincipal AppUser user) {
    return ResponseEntity.ok(groupService.createGroup(request, user));
  }

  @PatchMapping("/{groupId}")
  public ResponseEntity<GroupResponse> updateGroup(
      @PathVariable UUID groupId,
      @Valid @RequestBody UpdateGroupRequest request,
      @AuthenticationPrincipal AppUser user) {
    return ResponseEntity.ok(groupService.updateGroup(groupId, request, user));
  }

  @DeleteMapping("/{groupId}")
  public ResponseEntity<Void> deleteGroup(
      @PathVariable UUID groupId, @AuthenticationPrincipal AppUser user) {
    groupService.deleteGroup(groupId, user);
    return ResponseEntity.noContent().build();
  }
}
