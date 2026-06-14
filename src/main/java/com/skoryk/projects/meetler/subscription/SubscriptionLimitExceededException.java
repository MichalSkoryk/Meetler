package com.skoryk.projects.meetler.subscription;

public class SubscriptionLimitExceededException extends RuntimeException {

  public SubscriptionLimitExceededException(String message) {
    super(message);
  }
}
