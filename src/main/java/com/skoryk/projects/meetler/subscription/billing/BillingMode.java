package com.skoryk.projects.meetler.subscription.billing;

public enum BillingMode {
  DISABLED,
  SANDBOX,
  PRODUCTION;

  public boolean accepts(BillingEnvironment environment) {
    return this != DISABLED && name().equals(environment.name());
  }
}
