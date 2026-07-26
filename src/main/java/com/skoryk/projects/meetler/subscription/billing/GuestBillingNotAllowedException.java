package com.skoryk.projects.meetler.subscription.billing;

public class GuestBillingNotAllowedException extends RuntimeException {
  public GuestBillingNotAllowedException() {
    super("Create an account before purchasing a Meetler plan");
  }
}
