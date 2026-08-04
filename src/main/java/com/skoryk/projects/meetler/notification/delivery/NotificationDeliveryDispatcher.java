package com.skoryk.projects.meetler.notification.delivery;

import com.skoryk.projects.meetler.notification.device.UserDevice;
import com.skoryk.projects.meetler.notification.device.UserDeviceRepository;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationDeliveryDispatcher {

  private static final int MAX_ATTEMPTS = 4;

  private final NotificationDeliveryRepository deliveryRepository;
  private final UserDeviceRepository userDeviceRepository;
  private final PushNotificationSender pushNotificationSender;

  @Value("${notification.delivery.retry-delay-ms:30000}")
  private long retryDelayMs;

  @Transactional
  public void dispatch(UUID deliveryId) {
    NotificationDelivery delivery = deliveryRepository.findById(deliveryId).orElse(null);
    if (delivery == null || delivery.getStatus() != NotificationDeliveryStatus.PENDING) {
      return;
    }

    UserDevice device = delivery.getDevice();
    if (device == null || !device.isEnabled() || device.getRevokedAt() != null) {
      delivery.setStatus(NotificationDeliveryStatus.SKIPPED);
      delivery.setLastError("Push device is disabled");
      delivery.setNextAttemptAt(null);
      deliveryRepository.save(delivery);
      return;
    }

    OffsetDateTime now = OffsetDateTime.now();
    PushDeliveryResult result = pushNotificationSender.send(device, delivery.getNotification());
    delivery.setAttemptCount(delivery.getAttemptCount() + 1);
    delivery.setLastAttemptAt(now);
    delivery.setLastError(truncate(result.error()));

    if (result.status() == NotificationDeliveryStatus.SENT) {
      delivery.setStatus(NotificationDeliveryStatus.SENT);
      delivery.setSentAt(now);
      delivery.setNextAttemptAt(null);
    } else if (result.invalidToken()) {
      delivery.setStatus(NotificationDeliveryStatus.FAILED);
      delivery.setNextAttemptAt(null);
      device.setEnabled(false);
      device.setRevokedAt(now);
      userDeviceRepository.save(device);
    } else if (result.retryable() && delivery.getAttemptCount() < MAX_ATTEMPTS) {
      delivery.setStatus(NotificationDeliveryStatus.PENDING);
      delivery.setNextAttemptAt(
          now.plusNanos(retryDelayMs * delivery.getAttemptCount() * 1_000_000));
    } else {
      delivery.setStatus(result.status());
      delivery.setNextAttemptAt(null);
    }
    deliveryRepository.save(delivery);
  }

  private String truncate(String error) {
    if (error == null || error.length() <= 2000) {
      return error;
    }
    return error.substring(0, 2000);
  }
}
