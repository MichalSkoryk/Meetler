package com.skoryk.projects.meetler.me;

import com.skoryk.projects.meetler.subscription.SubscriptionUsageResponse;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MeBootstrapResponse {
  private MeUserResponse user;
  private SubscriptionUsageResponse subscription;
  private ConnectedCalendarsResponse connectedCalendars;
}
