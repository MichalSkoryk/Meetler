package com.skoryk.projects.meetler.subscription;

import java.time.OffsetDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SubscriptionUsageResponse {
  private String planCode;
  private String planName;
  private int ownedGroupsUsed;
  private int maxOwnedGroups;
  private int availabilityTemplatesUsed;
  private int maxAvailabilityTemplates;
  private boolean billingConfigured;
  private boolean canPurchase;
  private boolean upgradeRequiresAccount;
  private String billingStatus;
  private String store;
  private Boolean willRenew;
  private OffsetDateTime renewsOrExpiresAt;
  private String managementUrl;
}
