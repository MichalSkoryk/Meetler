package com.skoryk.projects.meetler.notification;

import com.skoryk.projects.meetler.notification.dto.NotificationResponse;
import com.skoryk.projects.meetler.notification.dto.RegisterDeviceRequest;
import com.skoryk.projects.meetler.notification.dto.UnreadNotificationCountResponse;
import com.skoryk.projects.meetler.notification.dto.UserDeviceResponse;
import com.skoryk.projects.meetler.user.AppUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Notifications")
@RequestMapping("/api/me")
public interface NotificationApi {

  @Operation(
      summary = "List my notifications",
      description =
          "Lists notifications for the authenticated user. Use unreadOnly=true for unread items.")
  @GetMapping("/notifications")
  ResponseEntity<Page<NotificationResponse>> listNotifications(
      @RequestParam(defaultValue = "false") boolean unreadOnly,
      @PageableDefault(size = 50) Pageable pageable,
      @AuthenticationPrincipal AppUser user);

  @Operation(
      summary = "Count unread notifications",
      description = "Returns the number of unread notifications for the authenticated user.")
  @GetMapping("/notifications/unread-count")
  ResponseEntity<UnreadNotificationCountResponse> countUnreadNotifications(
      @AuthenticationPrincipal AppUser user);

  @Operation(summary = "Mark notification as read")
  @PatchMapping("/notifications/{notificationId}/read")
  ResponseEntity<NotificationResponse> markRead(
      @PathVariable UUID notificationId, @AuthenticationPrincipal AppUser user);

  @Operation(summary = "Mark all notifications as read")
  @PostMapping("/notifications/read-all")
  ResponseEntity<Void> markAllRead(@AuthenticationPrincipal AppUser user);

  @Operation(
      summary = "Register push device",
      description =
          "Registers or refreshes a mobile push device token for the authenticated user. Currently supports FCM tokens.")
  @PostMapping("/devices")
  ResponseEntity<UserDeviceResponse> registerDevice(
      @Valid @RequestBody RegisterDeviceRequest request, @AuthenticationPrincipal AppUser user);

  @Operation(summary = "Revoke push device")
  @DeleteMapping("/devices/{deviceId}")
  ResponseEntity<Void> revokeDevice(
      @PathVariable UUID deviceId, @AuthenticationPrincipal AppUser user);
}
