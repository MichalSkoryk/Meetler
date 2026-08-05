package com.skoryk.projects.meetler.notification;

import static org.assertj.core.api.Assertions.assertThat;

import com.skoryk.projects.meetler.notification.dto.NotificationResponse;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class NotificationMapperTest {

  private final NotificationMapper mapper = Mappers.getMapper(NotificationMapper.class);

  @Test
  void mapsNotificationAndDerivesReadFlag() {
    OffsetDateTime readAt = OffsetDateTime.parse("2026-08-05T18:00:00+02:00");
    Notification notification =
        Notification.builder()
            .id(UUID.randomUUID())
            .type(NotificationType.GROUP_EVENT_CONFIRMATION_REQUIRED)
            .title("Response needed")
            .body("Please respond")
            .groupId(UUID.randomUUID())
            .eventId(UUID.randomUUID())
            .readAt(readAt)
            .createdAt(OffsetDateTime.parse("2026-08-05T17:00:00+02:00"))
            .build();

    NotificationResponse response = mapper.toResponse(notification);

    assertThat(response.getId()).isEqualTo(notification.getId());
    assertThat(response.getType()).isEqualTo(NotificationType.GROUP_EVENT_CONFIRMATION_REQUIRED);
    assertThat(response.getGroupId()).isEqualTo(notification.getGroupId());
    assertThat(response.getEventId()).isEqualTo(notification.getEventId());
    assertThat(response.isRead()).isTrue();
    assertThat(response.getReadAt()).isEqualTo(readAt);
  }

  @Test
  void mapsUnreadNotification() {
    Notification notification = Notification.builder().readAt(null).build();

    assertThat(mapper.toResponse(notification).isRead()).isFalse();
  }
}
