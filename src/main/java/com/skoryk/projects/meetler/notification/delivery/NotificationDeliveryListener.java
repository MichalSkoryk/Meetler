package com.skoryk.projects.meetler.notification.delivery;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationDeliveryListener {

  private final NotificationDeliveryDispatcher dispatcher;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onDeliveryRequested(NotificationDeliveryRequested event) {
    try {
      dispatcher.dispatch(event.deliveryId());
    } catch (RuntimeException exception) {
      log.warn("Immediate push delivery failed for {}", event.deliveryId(), exception);
    }
  }
}
