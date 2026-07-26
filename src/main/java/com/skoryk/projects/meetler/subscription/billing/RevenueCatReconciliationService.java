package com.skoryk.projects.meetler.subscription.billing;

import com.skoryk.projects.meetler.user.AppUser;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RevenueCatReconciliationService {

  private final BillingProperties properties;
  private final RevenueCatClient revenueCatClient;
  private final BillingCustomerRepository customerRepository;
  private final BillingEntitlementRepository entitlementRepository;

  @Transactional
  public void synchronize(AppUser user) {
    if (!properties.isConfigured()) {
      throw new BillingUnavailableException("Billing is not configured");
    }

    RevenueCatSubscriberSnapshot snapshot = revenueCatClient.fetchSubscriber(user.getId());
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
    BillingCustomer customer =
        customerRepository
            .findByUser(user)
            .orElseGet(
                () ->
                    BillingCustomer.builder()
                        .user(user)
                        .revenueCatAppUserId(user.getId().toString())
                        .build());
    customer.setManagementUrl(snapshot.managementUrl());
    customer.setLastSyncedAt(now);
    customer = customerRepository.save(customer);

    List<BillingEntitlement> existing = entitlementRepository.findByCustomer(customer);
    existing.forEach(
        entitlement -> {
          entitlement.setStatus(BillingEntitlementStatus.EXPIRED);
          entitlement.setWillRenew(false);
        });
    Map<String, BillingEntitlement> existingBySource = new HashMap<>();
    existing.forEach(entitlement -> existingBySource.put(sourceKey(entitlement), entitlement));

    for (RevenueCatSubscriberSnapshot.Entitlement source : snapshot.entitlements()) {
      String planCode = properties.planCodeFor(source.entitlementId());
      if (planCode == null) {
        continue;
      }

      String sourceKey =
          sourceKey(source.entitlementId(), source.productId(), source.environment());
      BillingEntitlement entitlement =
          existingBySource.getOrDefault(
              sourceKey,
              BillingEntitlement.builder()
                  .customer(customer)
                  .entitlementId(source.entitlementId())
                  .productId(source.productId())
                  .environment(source.environment())
                  .build());
      BillingEntitlementStatus status = statusFor(source, now);
      entitlement.setPlanCode(planCode);
      entitlement.setStatus(status);
      entitlement.setStore(source.store());
      entitlement.setPurchasedAt(source.purchasedAt());
      entitlement.setExpiresAt(source.expiresAt());
      entitlement.setGracePeriodExpiresAt(source.gracePeriodExpiresAt());
      entitlement.setWillRenew(
          status != BillingEntitlementStatus.EXPIRED
              && status != BillingEntitlementStatus.CANCELLED
              && source.unsubscribeDetectedAt() == null
              && source.refundedAt() == null);
      existingBySource.put(sourceKey, entitlement);
    }

    entitlementRepository.saveAll(existingBySource.values());
  }

  private BillingEntitlementStatus statusFor(
      RevenueCatSubscriberSnapshot.Entitlement source, OffsetDateTime now) {
    if (source.refundedAt() != null) {
      return BillingEntitlementStatus.EXPIRED;
    }

    boolean graceActive =
        source.gracePeriodExpiresAt() != null && source.gracePeriodExpiresAt().isAfter(now);
    boolean expired =
        source.expiresAt() != null && !source.expiresAt().isAfter(now) && !graceActive;
    if (expired) {
      return BillingEntitlementStatus.EXPIRED;
    }
    if (source.billingIssueDetectedAt() != null) {
      return BillingEntitlementStatus.BILLING_ISSUE;
    }
    if (graceActive && source.expiresAt() != null && !source.expiresAt().isAfter(now)) {
      return BillingEntitlementStatus.GRACE_PERIOD;
    }
    if (source.unsubscribeDetectedAt() != null) {
      return BillingEntitlementStatus.CANCELLED;
    }
    return BillingEntitlementStatus.ACTIVE;
  }

  private String sourceKey(BillingEntitlement entitlement) {
    return sourceKey(
        entitlement.getEntitlementId(), entitlement.getProductId(), entitlement.getEnvironment());
  }

  private String sourceKey(String entitlementId, String productId, BillingEnvironment environment) {
    return entitlementId + "\u0000" + productId + "\u0000" + environment;
  }
}
