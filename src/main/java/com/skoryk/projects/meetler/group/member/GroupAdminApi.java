package com.skoryk.projects.meetler.group.member;

import com.skoryk.projects.meetler.user.AppUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Group Administration")
@RequestMapping("/api/groups/{groupId}/admin")
public interface GroupAdminApi {

  @Operation(
      summary = "Promote member to admin",
      description = "Promotes a regular group member to admin. Only the group owner can do this.")
  @PostMapping("/promote/{userId}")
  ResponseEntity<Void> promoteToAdmin(
      @PathVariable UUID groupId,
      @PathVariable UUID userId,
      @AuthenticationPrincipal AppUser requester);

  @Operation(
      summary = "Demote admin to member",
      description = "Demotes a group admin to regular member. Only the group owner can do this.")
  @PostMapping("/demote/{userId}")
  ResponseEntity<Void> demoteAdmin(
      @PathVariable UUID groupId,
      @PathVariable UUID userId,
      @AuthenticationPrincipal AppUser requester);

  @Operation(
      summary = "Remove a group member",
      description =
          "Removes a member from the group. Admins can remove members, while only the owner can remove admins. The owner cannot be removed.")
  @DeleteMapping("/remove/{userId}")
  ResponseEntity<Void> removeMember(
      @PathVariable UUID groupId,
      @PathVariable UUID userId,
      @AuthenticationPrincipal AppUser requester);

  @Operation(
      summary = "Transfer group ownership",
      description =
          "Transfers ownership to another active group member. The current owner becomes an admin.")
  @PostMapping("/transfer/{newOwnerId}")
  ResponseEntity<Void> transferOwnership(
      @PathVariable UUID groupId,
      @PathVariable UUID newOwnerId,
      @AuthenticationPrincipal AppUser requester);
}
