package com.skoryk.projects.meetler.notification.delivery;

import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.AndroidNotification;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.skoryk.projects.meetler.notification.Notification;
import com.skoryk.projects.meetler.notification.device.UserDevice;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "notification.firebase", name = "enabled", havingValue = "true")
public class FirebasePushNotificationSender implements PushNotificationSender {

  private static final EnumSet<MessagingErrorCode> TRANSIENT_ERRORS =
      EnumSet.of(
          MessagingErrorCode.INTERNAL,
          MessagingErrorCode.UNAVAILABLE,
          MessagingErrorCode.QUOTA_EXCEEDED);

  private final FirebaseMessaging firebaseMessaging;

  @Override
  public PushDeliveryResult send(UserDevice device, Notification notification) {
    try {
      firebaseMessaging.send(buildMessage(device, notification));
      return PushDeliveryResult.sent();
    } catch (FirebaseMessagingException exception) {
      MessagingErrorCode code = exception.getMessagingErrorCode();
      String message = code == null ? exception.getMessage() : code + ": " + exception.getMessage();
      if (code == MessagingErrorCode.UNREGISTERED || code == MessagingErrorCode.INVALID_ARGUMENT) {
        return PushDeliveryResult.invalidToken(message);
      }
      if (code != null && TRANSIENT_ERRORS.contains(code)) {
        return PushDeliveryResult.transientFailure(message);
      }
      return PushDeliveryResult.failed(message);
    }
  }

  Message buildMessage(UserDevice device, Notification notification) {
    Message.Builder builder =
        Message.builder()
            .setToken(device.getToken())
            .setNotification(
                com.google.firebase.messaging.Notification.builder()
                    .setTitle(notification.getTitle())
                    .setBody(notification.getBody())
                    .build())
            .setAndroidConfig(
                AndroidConfig.builder()
                    .setPriority(AndroidConfig.Priority.HIGH)
                    .setNotification(
                        AndroidNotification.builder().setChannelId("meetler-default").build())
                    .build())
            .putAllData(buildData(notification));
    return builder.build();
  }

  Map<String, String> buildData(Notification notification) {
    Map<String, String> data = new HashMap<>();
    data.put("notificationId", notification.getId().toString());
    data.put("type", notification.getType().name());
    if (notification.getGroupId() != null) {
      data.put("groupId", notification.getGroupId().toString());
    }
    if (notification.getEventId() != null) {
      data.put("eventId", notification.getEventId().toString());
    }
    return Map.copyOf(data);
  }
}
