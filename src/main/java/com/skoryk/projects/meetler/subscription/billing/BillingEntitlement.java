package com.skoryk.projects.meetler.subscription.billing;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "billing_entitlement")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillingEntitlement {

  @Id @GeneratedValue private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "billing_customer_id", nullable = false)
  private BillingCustomer customer;

  @Column(name = "entitlement_id", nullable = false, length = 128)
  private String entitlementId;

  @Column(name = "product_id", nullable = false)
  private String productId;

  @Column(name = "plan_code", nullable = false, length = 64)
  private String planCode;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private BillingEntitlementStatus status;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private BillingEnvironment environment;

  @Column(length = 64)
  private String store;

  @Column(name = "purchased_at")
  private OffsetDateTime purchasedAt;

  @Column(name = "expires_at")
  private OffsetDateTime expiresAt;

  @Column(name = "grace_period_expires_at")
  private OffsetDateTime gracePeriodExpiresAt;

  @Column(name = "will_renew", nullable = false)
  private boolean willRenew;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;

  public boolean isEffectiveAt(OffsetDateTime now, BillingMode mode) {
    if (!mode.accepts(environment) || status == BillingEntitlementStatus.EXPIRED) {
      return false;
    }
    OffsetDateTime accessEndsAt =
        gracePeriodExpiresAt != null && gracePeriodExpiresAt.isAfter(now)
            ? gracePeriodExpiresAt
            : expiresAt;
    return accessEndsAt == null || accessEndsAt.isAfter(now);
  }
}
