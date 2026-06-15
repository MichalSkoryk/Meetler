package com.skoryk.projects.meetler.group.event;

import com.skoryk.projects.meetler.group.event.dto.CreateGroupEventRequest;
import com.skoryk.projects.meetler.group.event.dto.ExportGroupEventResponse;
import com.skoryk.projects.meetler.group.event.dto.GroupEventResponse;
import com.skoryk.projects.meetler.group.event.dto.RespondToGroupEventRequest;
import com.skoryk.projects.meetler.group.event.dto.UpdateGroupEventRequest;
import com.skoryk.projects.meetler.group.event.sync.GoogleGroupEventExportService;
import com.skoryk.projects.meetler.user.AppUser;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class GroupEventController implements GroupEventApi {

  private final GroupEventService groupEventService;
  private final GoogleGroupEventExportService googleGroupEventExportService;

  @Override
  public ResponseEntity<GroupEventResponse> createEvent(
      UUID groupId, CreateGroupEventRequest request, AppUser user) {
    return ResponseEntity.ok(groupEventService.createEvent(groupId, request, user));
  }

  @Override
  public ResponseEntity<List<GroupEventResponse>> listEvents(
      UUID groupId, OffsetDateTime from, OffsetDateTime to, AppUser user) {
    return ResponseEntity.ok(groupEventService.listEvents(groupId, from, to, user));
  }

  @Override
  public ResponseEntity<GroupEventResponse> updateEvent(
      UUID groupId, UUID eventId, UpdateGroupEventRequest request, AppUser user) {
    return ResponseEntity.ok(groupEventService.updateEvent(groupId, eventId, request, user));
  }

  @Override
  public ResponseEntity<GroupEventResponse> respond(
      UUID groupId, UUID eventId, RespondToGroupEventRequest request, AppUser user) {
    return ResponseEntity.ok(groupEventService.respond(groupId, eventId, request, user));
  }

  @Override
  public ResponseEntity<Void> cancelEvent(UUID groupId, UUID eventId, AppUser user) {
    groupEventService.cancelEvent(groupId, eventId, user);
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<ExportGroupEventResponse> exportToGoogle(
      UUID groupId, UUID eventId, AppUser user) {
    return ResponseEntity.ok(googleGroupEventExportService.exportToGoogle(groupId, eventId, user));
  }
}
