package com.skoryk.projects.meetler.group.invite;

import com.skoryk.projects.meetler.group.invite.dto.GroupInviteResponse;
import com.skoryk.projects.meetler.user.AppUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Group Invites")
@RequestMapping("/api/invites")
public interface GroupInviteApi {

  @Operation(
      summary = "List active group invites",
      description =
          "Returns active invite codes for a group. The authenticated user must have owner or admin permission.")
  @GetMapping("/{groupId}")
  ResponseEntity<List<GroupInviteResponse>> listActiveInvites(
      @PathVariable UUID groupId, @AuthenticationPrincipal AppUser user);

  @Operation(
      summary = "Create a group invite",
      description =
          "Creates an invite code for a group. The authenticated user must have owner or admin permission.")
  @PostMapping("/{groupId}")
  ResponseEntity<GroupInviteResponse> createInvite(
      @PathVariable UUID groupId,
      @Valid @RequestBody GroupInviteController.InviteRequest request,
      @AuthenticationPrincipal AppUser user);

  @Operation(
      summary = "Revoke a group invite",
      description =
          "Revokes an invite code so it can no longer be used. The authenticated user must have owner or admin permission in the invite's group.")
  @PatchMapping("/{inviteId}/revoke")
  ResponseEntity<GroupInviteResponse> revokeInvite(
      @PathVariable UUID inviteId, @AuthenticationPrincipal AppUser user);

  @Operation(
      summary = "Join group with invite code",
      description = "Adds the authenticated user to the group associated with the invite code.")
  @PostMapping("/join/{code}")
  ResponseEntity<Void> joinWithCode(
      @PathVariable String code, @AuthenticationPrincipal AppUser user);
}
