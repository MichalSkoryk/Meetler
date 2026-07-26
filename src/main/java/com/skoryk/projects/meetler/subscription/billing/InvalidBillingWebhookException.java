package com.skoryk.projects.meetler.subscription.billing;

public class InvalidBillingWebhookException extends RuntimeException {
  public InvalidBillingWebhookException(String message) {
    super(message);
  }
}
