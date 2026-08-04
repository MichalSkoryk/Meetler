package com.skoryk.projects.meetler.notification.delivery;

import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationDeliveryRetryJob {

  private final NotificationDeliveryRepository deliveryRepository;
  private final NotificationDeliveryDispatcher dispatcher;

  @Scheduled(fixedDelayString = "${notification.delivery.scheduler-delay-ms:15000}")
  public void retryReadyDeliveries() {
    deliveryRepository
        .findReadyDeliveryIds(OffsetDateTime.now(), PageRequest.of(0, 100))
        .forEach(dispatcher::dispatch);
  }
}
