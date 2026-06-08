package com.skoryk.projects.meetler.group.member;

import com.skoryk.projects.meetler.group.member.dto.GroupAvailabilityTemplateResponse;
import com.skoryk.projects.meetler.group.member.dto.GroupMemberResponse;
import com.skoryk.projects.meetler.group.member.dto.SelectGroupAvailabilityTemplateRequest;
import com.skoryk.projects.meetler.user.AppUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
  ResponseEntity<List<GroupMemberResponse>> listMembers(@PathVariable UUID groupId);

  @Operation(
      summary = "Get my group availability template",
      description =
          "Returns which availability template the authenticated group member uses in this group.")
  @GetMapping("/me/availability-template")
  ResponseEntity<GroupAvailabilityTemplateResponse> getMyAvailabilityTemplate(
      @PathVariable UUID groupId, @AuthenticationPrincipal AppUser user);

  @Operation(
      summary = "Select my group availability template",
      description =
          "Selects one of the authenticated user's availability templates to represent them in this group.")
  @PostMapping("/me/availability-template")
  ResponseEntity<GroupAvailabilityTemplateResponse> selectMyAvailabilityTemplate(
      @PathVariable UUID groupId,
      @Valid @RequestBody SelectGroupAvailabilityTemplateRequest request,
      @AuthenticationPrincipal AppUser user);

  @Operation(
      summary = "Clear my group availability template",
      description =
          "Clears the authenticated member's selected availability template for this group.")
  @DeleteMapping("/me/availability-template")
  ResponseEntity<GroupAvailabilityTemplateResponse> clearMyAvailabilityTemplate(
      @PathVariable UUID groupId, @AuthenticationPrincipal AppUser user);
}
