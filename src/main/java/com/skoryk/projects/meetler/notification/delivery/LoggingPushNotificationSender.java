package com.skoryk.projects.meetler.notification.delivery;

import com.skoryk.projects.meetler.notification.Notification;
import com.skoryk.projects.meetler.notification.device.UserDevice;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
    prefix = "notification.firebase",
    name = "enabled",
    havingValue = "false",
    matchIfMissing = true)
@Slf4j
public class LoggingPushNotificationSender implements PushNotificationSender {

  @Override
  public PushDeliveryResult send(UserDevice device, Notification notification) {
    log.info(
        "Push notification skipped because FCM is not configured yet. userId={}, deviceId={}, notificationId={}, type={}",
        notification.getUser().getId(),
        device.getId(),
        notification.getId(),
        notification.getType());
    return PushDeliveryResult.skipped("Push provider is not configured");
  }
}
