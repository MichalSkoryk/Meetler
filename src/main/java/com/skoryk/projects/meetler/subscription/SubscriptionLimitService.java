package com.skoryk.projects.meetler.subscription;

import com.skoryk.projects.meetler.user.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SubscriptionLimitService {

  private final SubscriptionUsageService subscriptionUsageService;

  public void assertCanCreateGroup(AppUser user) {
    SubscriptionUsageResponse subscriptionUsageResponse = subscriptionUsageService.getUsage(user);

    if (subscriptionUsageResponse.getOwnedGroupsUsed()
        >= subscriptionUsageResponse.getMaxOwnedGroups()) {
      throw new SubscriptionLimitExceededException("Limit of created groups was reached");
    }
  }

  public void assertCanCreateAvailabilityTemplate(AppUser user) {
    SubscriptionUsageResponse subscriptionUsageResponse = subscriptionUsageService.getUsage(user);

    if (subscriptionUsageResponse.getAvailabilityTemplatesUsed()
        >= subscriptionUsageResponse.getMaxAvailabilityTemplates()) {

      throw new SubscriptionLimitExceededException(
          "Limit of created availability templates was reached");
    }
  }
}
