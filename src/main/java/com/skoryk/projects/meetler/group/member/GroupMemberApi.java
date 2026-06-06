package com.skoryk.projects.meetler.group.member;

import com.skoryk.projects.meetler.user.AppUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Group Members")
@Validated
@RequestMapping("/api/groups/{groupId}/members")
public interface GroupMemberApi {

  @Operation(
      summary = "Join a group",
      description =
          "Adds the authenticated user to the selected group by validating the supplied invite code.")
  @PostMapping("/join")
  ResponseEntity<Void> joinGroup(
      @PathVariable UUID groupId,
      @NotBlank @RequestParam String code,
      @AuthenticationPrincipal AppUser user);

  @Operation(
      summary = "Leave a group",
      description =
          "Removes the authenticated user from the group. Owners must transfer ownership before leaving.")
  @DeleteMapping("/leave")
  ResponseEntity<Void> leaveGroup(
      @PathVariable UUID groupId, @AuthenticationPrincipal AppUser user);

  @Operation(
      summary = "List group members",
      description = "Returns the members and roles for the selected group.")
  @GetMapping
  ResponseEntity<?> listMembers(@PathVariable UUID groupId);
}
