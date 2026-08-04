package com.skoryk.projects.meetler.notification.delivery;

public record PushDeliveryResult(
    NotificationDeliveryStatus status, String error, boolean retryable, boolean invalidToken) {

  public static PushDeliveryResult sent() {
    return new PushDeliveryResult(NotificationDeliveryStatus.SENT, null, false, false);
  }

  public static PushDeliveryResult skipped(String reason) {
    return new PushDeliveryResult(NotificationDeliveryStatus.SKIPPED, reason, false, false);
  }

  public static PushDeliveryResult failed(String error) {
    return new PushDeliveryResult(NotificationDeliveryStatus.FAILED, error, false, false);
  }

  public static PushDeliveryResult transientFailure(String error) {
    return new PushDeliveryResult(NotificationDeliveryStatus.FAILED, error, true, false);
  }

  public static PushDeliveryResult invalidToken(String error) {
    return new PushDeliveryResult(NotificationDeliveryStatus.FAILED, error, false, true);
  }
}
