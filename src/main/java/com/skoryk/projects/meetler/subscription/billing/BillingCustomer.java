package com.skoryk.projects.meetler.subscription.billing;

import com.skoryk.projects.meetler.user.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
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
@Table(name = "billing_customer")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillingCustomer {

  @Id @GeneratedValue private UUID id;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false, unique = true)
  private AppUser user;

  @Column(name = "revenuecat_app_user_id", nullable = false, unique = true)
  private String revenueCatAppUserId;

  @Column(name = "management_url")
  private String managementUrl;

  @Column(name = "last_synced_at")
  private OffsetDateTime lastSyncedAt;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;
}
