package com.skoryk.projects.meetler.group.event;

import com.skoryk.projects.meetler.group.event.dto.CreateGroupEventRequest;
import com.skoryk.projects.meetler.group.event.dto.ExportGroupEventResponse;
import com.skoryk.projects.meetler.group.event.dto.GroupEventResponse;
import com.skoryk.projects.meetler.group.event.dto.RespondToGroupEventRequest;
import com.skoryk.projects.meetler.group.event.dto.UpdateGroupEventRequest;
import com.skoryk.projects.meetler.user.AppUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Group Events")
@RequestMapping("/api/groups/{groupId}/events")
public interface GroupEventApi {

  @Operation(
      summary = "Create a group event",
      description =
          "Creates a group event. The event starts as pending or confirmed depending on the group confirmation setting.")
  @PostMapping
  ResponseEntity<GroupEventResponse> createEvent(
      @PathVariable UUID groupId,
      @Valid @RequestBody CreateGroupEventRequest request,
      @AuthenticationPrincipal AppUser user);

  @Operation(
      summary = "List group events",
      description = "Lists group events overlapping the supplied date-time range.")
  @GetMapping
  ResponseEntity<List<GroupEventResponse>> listEvents(
      @PathVariable UUID groupId,
      @RequestParam OffsetDateTime from,
      @RequestParam OffsetDateTime to,
      @AuthenticationPrincipal AppUser user);

  @Operation(
      summary = "Update group event",
      description =
          "Updates information about the group event. Title, description, start time and end time can be changed.")
  @PatchMapping("/{eventId}")
  ResponseEntity<GroupEventResponse> updateEvent(
      @PathVariable UUID groupId,
      @PathVariable UUID eventId,
      @Valid @RequestBody UpdateGroupEventRequest request,
      @AuthenticationPrincipal AppUser user);

  @Operation(
      summary = "Respond to a group event",
      description =
          "Accepts or declines a pending group event for the authenticated member. The event becomes confirmed when all participants have accepted.")
  @PatchMapping("/{eventId}/response")
  ResponseEntity<GroupEventResponse> respond(
      @PathVariable UUID groupId,
      @PathVariable UUID eventId,
      @Valid @RequestBody RespondToGroupEventRequest request,
      @AuthenticationPrincipal AppUser user);

  @Operation(summary = "Cancel the event", description = "Cancels the event")
  @DeleteMapping("/{eventId}")
  ResponseEntity<Void> cancelEvent(
      @PathVariable UUID groupId,
      @PathVariable UUID eventId,
      @AuthenticationPrincipal AppUser appUser);

  @Operation(
      summary = "Export group event to Google Calendar",
      description =
          "Creates or updates the event in the authenticated user's Google calendar named Meetler.")
  @PostMapping("/{eventId}/google/export")
  ResponseEntity<ExportGroupEventResponse> exportToGoogle(
      @PathVariable UUID groupId,
      @PathVariable UUID eventId,
      @AuthenticationPrincipal AppUser appUser);
}
