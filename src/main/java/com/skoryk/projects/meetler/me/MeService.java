package com.skoryk.projects.meetler.me;

import com.skoryk.projects.meetler.calendar.CalendarProvider;
import com.skoryk.projects.meetler.calendar.external.ExternalCalendarAccountRepository;
import com.skoryk.projects.meetler.subscription.SubscriptionUsageResponse;
import com.skoryk.projects.meetler.subscription.SubscriptionUsageService;
import com.skoryk.projects.meetler.user.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MeService {

  private final SubscriptionUsageService subscriptionUsageService;
  private final ExternalCalendarAccountRepository externalCalendarAccountRepository;

  public MeBootstrapResponse getBootstrap(AppUser user) {
    return MeBootstrapResponse.builder()
        .user(toUserResponse(user))
        .subscription(getSubscriptionUsage(user))
        .connectedCalendars(getConnectedCalendars(user))
        .build();
  }

  public SubscriptionUsageResponse getSubscriptionUsage(AppUser user) {
    return subscriptionUsageService.getUsage(user);
  }

  private MeUserResponse toUserResponse(AppUser user) {
    return MeUserResponse.builder()
        .id(user.getId())
        .email(user.getEmail())
        .name(user.getName())
        .role(user.getRole())
        .hasPassword(user.getPasswordHash() != null && !user.getPasswordHash().isBlank())
        .build();
  }

  private ConnectedCalendarsResponse getConnectedCalendars(AppUser user) {
    return ConnectedCalendarsResponse.builder()
        .google(hasProvider(user, CalendarProvider.GOOGLE))
        .microsoft(hasProvider(user, CalendarProvider.TEAMS))
        .build();
  }

  private boolean hasProvider(AppUser user, CalendarProvider provider) {
    return !externalCalendarAccountRepository
        .findByUserAndProviderAndRevokedAtIsNull(user, provider)
        .isEmpty();
  }
}
