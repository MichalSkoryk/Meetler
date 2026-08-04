package com.skoryk.projects.meetler.notification;

import com.skoryk.projects.meetler.group.event.GroupEvent;
import com.skoryk.projects.meetler.group.event.GroupEventParticipant;
import com.skoryk.projects.meetler.notification.delivery.*;
import com.skoryk.projects.meetler.notification.device.UserDevice;
import com.skoryk.projects.meetler.notification.device.UserDeviceRepository;
import com.skoryk.projects.meetler.notification.dto.NotificationResponse;
import com.skoryk.projects.meetler.notification.dto.UnreadNotificationCountResponse;
import com.skoryk.projects.meetler.user.AppUser;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {

  private final NotificationRepository notificationRepository;
  private final UserDeviceRepository userDeviceRepository;
  private final NotificationDeliveryRepository deliveryRepository;
  private final ApplicationEventPublisher eventPublisher;

  @Transactional(readOnly = true)
  public Page<NotificationResponse> listNotifications(
      AppUser user, boolean unreadOnly, Pageable pageable) {
    Page<Notification> notifications =
        unreadOnly
            ? notificationRepository.findByUserAndReadAtIsNullOrderByCreatedAtDesc(user, pageable)
            : notificationRepository.findByUserOrderByCreatedAtDesc(user, pageable);
    return notifications.map(this::toResponse);
  }

  @Transactional
  public NotificationResponse markRead(AppUser user, UUID notificationId) {
    Notification notification =
        notificationRepository
            .findById(notificationId)
            .orElseThrow(() -> new IllegalArgumentException("Notification not found"));
    if (!notification.getUser().getId().equals(user.getId())) {
      throw new IllegalArgumentException("Notification not found");
    }
    if (notification.getReadAt() == null) {
      notification.setReadAt(OffsetDateTime.now());
      notificationRepository.save(notification);
    }
    return toResponse(notification);
  }

  @Transactional
  public void markAllRead(AppUser user) {
    notificationRepository.markAllUnreadAsRead(user);
  }

  @Transactional(readOnly = true)
  public UnreadNotificationCountResponse countUnread(AppUser user) {
    return UnreadNotificationCountResponse.builder()
        .unreadCount(notificationRepository.countByUserAndReadAtIsNull(user))
        .build();
  }

  @Transactional
  public void notifyGroupEventCreated(GroupEvent event, List<GroupEventParticipant> participants) {
    for (GroupEventParticipant participant : participants) {
      AppUser recipient = participant.getUser();
      if (recipient.getId().equals(event.getCreatedBy().getId())) {
        continue;
      }

      NotificationType type =
          event.isRequiresConfirmation()
              ? NotificationType.GROUP_EVENT_CONFIRMATION_REQUIRED
              : NotificationType.GROUP_EVENT_CREATED;
      String title =
          event.isRequiresConfirmation() ? "Event needs your confirmation" : "New group event";
      String body =
          event.isRequiresConfirmation()
              ? "Please accept or decline: " + event.getTitle()
              : event.getTitle() + " was added to your group.";
      createAndPush(recipient, type, title, body, event);
    }
  }

  @Transactional
  public void notifyGroupEventUpdated(GroupEvent event, List<GroupEventParticipant> participants) {
    for (GroupEventParticipant participant : participants) {
      if (participant.getUser().getId().equals(event.getCreatedBy().getId())) {
        continue;
      }
      createAndPush(
          participant.getUser(),
          NotificationType.GROUP_EVENT_UPDATED,
          "Group event updated",
          event.getTitle() + " was updated.",
          event);
    }
  }

  @Transactional
  public void notifyGroupEventCancelled(
      GroupEvent event, List<GroupEventParticipant> participants, AppUser cancelledBy) {
    for (GroupEventParticipant participant : participants) {
      if (participant.getUser().getId().equals(cancelledBy.getId())) {
        continue;
      }
      createAndPush(
          participant.getUser(),
          NotificationType.GROUP_EVENT_CANCELLED,
          "Group event cancelled",
          event.getTitle() + " was cancelled.",
          event);
    }
  }

  @Transactional
  public void notifyGroupEventConfirmed(
      GroupEvent event, List<GroupEventParticipant> participants) {
    for (GroupEventParticipant participant : participants) {
      createAndPush(
          participant.getUser(),
          NotificationType.GROUP_EVENT_CONFIRMED,
          "Group event confirmed",
          event.getTitle() + " was confirmed.",
          event);
    }
  }

  private void createAndPush(
      AppUser recipient, NotificationType type, String title, String body, GroupEvent event) {
    Notification notification =
        notificationRepository.save(
            Notification.builder()
                .user(recipient)
                .type(type)
                .title(title)
                .body(body)
                .groupId(event.getGroup().getId())
                .eventId(event.getId())
                .build());

    List<UserDevice> devices =
        userDeviceRepository.findByUserAndEnabledTrueAndRevokedAtIsNull(recipient);
    for (UserDevice device : devices) {
      NotificationDelivery delivery =
          NotificationDelivery.builder()
              .notification(notification)
              .device(device)
              .channel(NotificationChannel.PUSH)
              .status(NotificationDeliveryStatus.PENDING)
              .build();

      NotificationDelivery savedDelivery = deliveryRepository.save(delivery);
      eventPublisher.publishEvent(new NotificationDeliveryRequested(savedDelivery.getId()));
    }
  }

  private NotificationResponse toResponse(Notification notification) {
    return NotificationResponse.builder()
        .id(notification.getId())
        .type(notification.getType())
        .title(notification.getTitle())
        .body(notification.getBody())
        .groupId(notification.getGroupId())
        .eventId(notification.getEventId())
        .read(notification.getReadAt() != null)
        .readAt(notification.getReadAt())
        .createdAt(notification.getCreatedAt())
        .build();
  }
}
