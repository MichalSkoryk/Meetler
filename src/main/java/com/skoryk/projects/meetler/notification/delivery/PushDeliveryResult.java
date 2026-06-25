package com.skoryk.projects.meetler.notification.delivery;

public record PushDeliveryResult(NotificationDeliveryStatus status, String error) {

  public static PushDeliveryResult sent() {
    return new PushDeliveryResult(NotificationDeliveryStatus.SENT, null);
  }

  public static PushDeliveryResult skipped(String reason) {
    return new PushDeliveryResult(NotificationDeliveryStatus.SKIPPED, reason);
  }

  public static PushDeliveryResult failed(String error) {
    return new PushDeliveryResult(NotificationDeliveryStatus.FAILED, error);
  }
}
