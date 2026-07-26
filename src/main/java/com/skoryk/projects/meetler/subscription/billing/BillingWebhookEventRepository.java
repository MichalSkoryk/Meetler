package com.skoryk.projects.meetler.subscription.billing;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BillingWebhookEventRepository extends JpaRepository<BillingWebhookEvent, UUID> {
  boolean existsByRevenueCatEventId(String revenueCatEventId);
}
