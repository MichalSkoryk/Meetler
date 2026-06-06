package com.skoryk.projects.meetler.group.invite;

import com.skoryk.projects.meetler.user.AppUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Group Invites")
@RequestMapping("/api/invites")
public interface GroupInviteApi {

  @Operation(
      summary = "Create a group invite",
      description =
          "Creates an invite code for a group. The authenticated user must have owner or admin permission.")
  @PostMapping("/{groupId}")
  ResponseEntity<Map<String, String>> createInvite(
      @PathVariable UUID groupId,
      @Valid @RequestBody GroupInviteController.InviteRequest request,
      @AuthenticationPrincipal AppUser user);

  @Operation(
      summary = "Join group with invite code",
      description = "Adds the authenticated user to the group associated with the invite code.")
  @PostMapping("/join/{code}")
  ResponseEntity<Void> joinWithCode(
      @PathVariable String code, @AuthenticationPrincipal AppUser user);
}
