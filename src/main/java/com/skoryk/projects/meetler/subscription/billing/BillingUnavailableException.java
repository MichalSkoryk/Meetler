package com.skoryk.projects.meetler.subscription.billing;

public class BillingUnavailableException extends RuntimeException {
  public BillingUnavailableException(String message) {
    super(message);
  }
}
