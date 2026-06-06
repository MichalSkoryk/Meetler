package com.skoryk.projects.meetler.group;

import com.skoryk.projects.meetler.group.dto.CreateGroupRequest;
import com.skoryk.projects.meetler.group.dto.GroupResponse;
import com.skoryk.projects.meetler.group.dto.UpdateGroupRequest;
import com.skoryk.projects.meetler.user.AppUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Groups")
@RequestMapping("/api/groups")
public interface GroupApi {

  @Operation(
      summary = "Create a group",
      description = "Creates a new group and makes the authenticated user its owner.")
  @PostMapping
  ResponseEntity<GroupResponse> createGroup(
      @Valid @RequestBody CreateGroupRequest request, @AuthenticationPrincipal AppUser user);

  @Operation(
      summary = "Update a group",
      description =
          "Updates group metadata. The authenticated user must be allowed to manage the group.")
  @PatchMapping("/{groupId}")
  ResponseEntity<GroupResponse> updateGroup(
      @PathVariable UUID groupId,
      @Valid @RequestBody UpdateGroupRequest request,
      @AuthenticationPrincipal AppUser user);

  @Operation(
      summary = "Delete a group",
      description = "Deletes a group. The authenticated user must be allowed to delete it.")
  @DeleteMapping("/{groupId}")
  ResponseEntity<Void> deleteGroup(
      @PathVariable UUID groupId, @AuthenticationPrincipal AppUser user);
}
