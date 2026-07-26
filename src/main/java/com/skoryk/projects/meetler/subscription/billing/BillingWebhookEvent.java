package com.skoryk.projects.meetler.subscription.billing;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "billing_webhook_event")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillingWebhookEvent {

  @Id @GeneratedValue private UUID id;

  @Column(name = "revenuecat_event_id", nullable = false, unique = true)
  private String revenueCatEventId;

  @Column(name = "app_user_id")
  private String appUserId;

  @Column(name = "event_type", nullable = false, length = 64)
  private String eventType;

  @Column(length = 16)
  private String environment;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String payload;

  @CreationTimestamp
  @Column(name = "received_at", nullable = false)
  private OffsetDateTime receivedAt;

  @Column(name = "processed_at", nullable = false)
  private OffsetDateTime processedAt;
}
