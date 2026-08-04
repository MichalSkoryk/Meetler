package com.skoryk.projects.meetler.notification.delivery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.skoryk.projects.meetler.notification.Notification;
import com.skoryk.projects.meetler.notification.NotificationType;
import com.skoryk.projects.meetler.notification.device.UserDevice;
import com.skoryk.projects.meetler.notification.device.UserDeviceRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class NotificationDeliveryDispatcherTest {

  @Mock private NotificationDeliveryRepository deliveryRepository;
  @Mock private UserDeviceRepository userDeviceRepository;
  @Mock private PushNotificationSender pushNotificationSender;
  @InjectMocks private NotificationDeliveryDispatcher dispatcher;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(dispatcher, "retryDelayMs", 1000L);
  }

  @Test
  void transientFailuresAreRetriedThreeTimesThenFail() {
    NotificationDelivery delivery = delivery();
    when(deliveryRepository.findById(delivery.getId())).thenReturn(Optional.of(delivery));
    when(pushNotificationSender.send(delivery.getDevice(), delivery.getNotification()))
        .thenReturn(PushDeliveryResult.transientFailure("FCM unavailable"));

    dispatcher.dispatch(delivery.getId());
    assertThat(delivery.getStatus()).isEqualTo(NotificationDeliveryStatus.PENDING);
    assertThat(delivery.getAttemptCount()).isEqualTo(1);
    assertThat(delivery.getNextAttemptAt()).isNotNull();

    dispatcher.dispatch(delivery.getId());
    dispatcher.dispatch(delivery.getId());
    dispatcher.dispatch(delivery.getId());

    assertThat(delivery.getAttemptCount()).isEqualTo(4);
    assertThat(delivery.getStatus()).isEqualTo(NotificationDeliveryStatus.FAILED);
    assertThat(delivery.getNextAttemptAt()).isNull();
  }

  @Test
  void invalidTokenDisablesDevice() {
    NotificationDelivery delivery = delivery();
    when(deliveryRepository.findById(delivery.getId())).thenReturn(Optional.of(delivery));
    when(pushNotificationSender.send(delivery.getDevice(), delivery.getNotification()))
        .thenReturn(PushDeliveryResult.invalidToken("UNREGISTERED"));

    dispatcher.dispatch(delivery.getId());

    assertThat(delivery.getStatus()).isEqualTo(NotificationDeliveryStatus.FAILED);
    assertThat(delivery.getDevice().isEnabled()).isFalse();
    assertThat(delivery.getDevice().getRevokedAt()).isNotNull();
    verify(userDeviceRepository).save(delivery.getDevice());
  }

  private NotificationDelivery delivery() {
    Notification notification =
        Notification.builder()
            .id(UUID.randomUUID())
            .type(NotificationType.GROUP_EVENT_UPDATED)
            .title("Updated")
            .body("Dinner was updated")
            .build();
    UserDevice device =
        UserDevice.builder().id(UUID.randomUUID()).token("fcm").enabled(true).build();
    return NotificationDelivery.builder()
        .id(UUID.randomUUID())
        .notification(notification)
        .device(device)
        .channel(NotificationChannel.PUSH)
        .status(NotificationDeliveryStatus.PENDING)
        .build();
  }
}
