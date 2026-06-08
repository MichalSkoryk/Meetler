package com.skoryk.projects.meetler.group;

import com.skoryk.projects.meetler.group.dto.CreateGroupRequest;
import com.skoryk.projects.meetler.group.dto.GroupAvailabilitySlotResponse;
import com.skoryk.projects.meetler.group.dto.GroupResponse;
import com.skoryk.projects.meetler.group.dto.UpdateGroupRequest;
import com.skoryk.projects.meetler.user.AppUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

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
      summary = "List my groups",
      description =
          "Returns groups where the authenticated user is a member, including their role.")
  @GetMapping
  ResponseEntity<List<GroupResponse>> getMyGroups(@AuthenticationPrincipal AppUser user);

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

  @Operation(
      summary = "Get group availability grid",
      description =
          "Returns 15-minute availability slots for the selected group and date-time range. "
              + "Each member is counted as available, busy, or missing a selected availability template.")
  @GetMapping("/{groupId}/availability-grid")
  ResponseEntity<List<GroupAvailabilitySlotResponse>> getAvailabilityGrid(
      @PathVariable UUID groupId,
      @RequestParam OffsetDateTime from,
      @RequestParam OffsetDateTime to,
      @AuthenticationPrincipal AppUser user);
}
