package com.skoryk.projects.meetler.subscription.billing;

import java.time.OffsetDateTime;
import java.util.List;

public record RevenueCatSubscriberSnapshot(
    String originalAppUserId, String managementUrl, List<Entitlement> entitlements) {

  public record Entitlement(
      String entitlementId,
      String productId,
      String store,
      BillingEnvironment environment,
      OffsetDateTime purchasedAt,
      OffsetDateTime expiresAt,
      OffsetDateTime gracePeriodExpiresAt,
      OffsetDateTime unsubscribeDetectedAt,
      OffsetDateTime billingIssueDetectedAt,
      OffsetDateTime refundedAt) {}
}
