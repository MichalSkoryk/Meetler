package com.skoryk.projects.meetler.notification.delivery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.google.firebase.messaging.FirebaseMessaging;
import com.skoryk.projects.meetler.notification.Notification;
import com.skoryk.projects.meetler.notification.NotificationType;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FirebasePushNotificationSenderTest {

  @Test
  void payloadContainsIdsRequiredForMobileRouting() {
    UUID notificationId = UUID.randomUUID();
    UUID groupId = UUID.randomUUID();
    UUID eventId = UUID.randomUUID();
    Notification notification =
        Notification.builder()
            .id(notificationId)
            .type(NotificationType.GROUP_EVENT_CONFIRMATION_REQUIRED)
            .title("Respond")
            .body("Please respond")
            .groupId(groupId)
            .eventId(eventId)
            .build();
    FirebasePushNotificationSender sender =
        new FirebasePushNotificationSender(mock(FirebaseMessaging.class));

    assertThat(sender.buildData(notification))
        .containsEntry("notificationId", notificationId.toString())
        .containsEntry("groupId", groupId.toString())
        .containsEntry("eventId", eventId.toString())
        .containsEntry("type", NotificationType.GROUP_EVENT_CONFIRMATION_REQUIRED.name());
  }
}
