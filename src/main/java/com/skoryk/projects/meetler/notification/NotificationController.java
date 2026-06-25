package com.skoryk.projects.meetler.notification;

import com.skoryk.projects.meetler.notification.device.UserDeviceService;
import com.skoryk.projects.meetler.notification.dto.NotificationResponse;
import com.skoryk.projects.meetler.notification.dto.RegisterDeviceRequest;
import com.skoryk.projects.meetler.notification.dto.UnreadNotificationCountResponse;
import com.skoryk.projects.meetler.notification.dto.UserDeviceResponse;
import com.skoryk.projects.meetler.user.AppUser;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class NotificationController implements NotificationApi {

  private final NotificationService notificationService;
  private final UserDeviceService userDeviceService;

  @Override
  public ResponseEntity<Page<NotificationResponse>> listNotifications(
      boolean unreadOnly, Pageable pageable, AppUser user) {
    return ResponseEntity.ok(notificationService.listNotifications(user, unreadOnly, pageable));
  }

  @Override
  public ResponseEntity<UnreadNotificationCountResponse> countUnreadNotifications(AppUser user) {
    return ResponseEntity.ok(notificationService.countUnread(user));
  }

  @Override
  public ResponseEntity<NotificationResponse> markRead(UUID notificationId, AppUser user) {
    return ResponseEntity.ok(notificationService.markRead(user, notificationId));
  }

  @Override
  public ResponseEntity<Void> markAllRead(AppUser user) {
    notificationService.markAllRead(user);
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<UserDeviceResponse> registerDevice(
      RegisterDeviceRequest request, AppUser user) {
    return ResponseEntity.ok(userDeviceService.registerDevice(user, request));
  }

  @Override
  public ResponseEntity<Void> revokeDevice(UUID deviceId, AppUser user) {
    userDeviceService.revokeDevice(user, deviceId);
    return ResponseEntity.noContent().build();
  }
}
