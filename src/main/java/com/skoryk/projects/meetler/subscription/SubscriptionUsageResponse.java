package com.skoryk.projects.meetler.subscription;

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
}
