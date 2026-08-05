package com.skoryk.projects.meetler.me;

import com.skoryk.projects.meetler.auth.identity.UserAuthIdentityRepository;
import com.skoryk.projects.meetler.calendar.CalendarProvider;
import com.skoryk.projects.meetler.calendar.external.ExternalCalendarAccountRepository;
import com.skoryk.projects.meetler.subscription.SubscriptionUsageResponse;
import com.skoryk.projects.meetler.subscription.SubscriptionUsageService;
import com.skoryk.projects.meetler.subscription.billing.BillingSynchronizationService;
import com.skoryk.projects.meetler.user.AppUser;
import com.skoryk.projects.meetler.user.AuthProvider;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MeService {

  private final SubscriptionUsageService subscriptionUsageService;
  private final BillingSynchronizationService billingSynchronizationService;
  private final ExternalCalendarAccountRepository externalCalendarAccountRepository;
  private final UserAuthIdentityRepository userAuthIdentityRepository;
  private final MeMapper meMapper;

  public MeBootstrapResponse getBootstrap(AppUser user) {
    return meMapper.toBootstrapResponse(
        meMapper.toUserResponse(user),
        getSubscriptionUsage(user),
        getConnectedCalendars(user),
        getConnectedLoginMethods(user));
  }

  public SubscriptionUsageResponse getSubscriptionUsage(AppUser user) {
    return subscriptionUsageService.getUsage(user);
  }

  public SubscriptionUsageResponse synchronizeSubscription(AppUser user) {
    billingSynchronizationService.synchronizeCurrentUser(user);
    return subscriptionUsageService.getUsage(user);
  }

  private ConnectedCalendarsResponse getConnectedCalendars(AppUser user) {
    return meMapper.toConnectedCalendarsResponse(
        hasProvider(user, CalendarProvider.GOOGLE), hasProvider(user, CalendarProvider.TEAMS));
  }

  private ConnectedLoginMethodsResponse getConnectedLoginMethods(AppUser user) {
    Set<AuthProvider> providers =
        userAuthIdentityRepository.findByUser(user).stream()
            .map(identity -> identity.getProvider())
            .collect(Collectors.toSet());

    return meMapper.toConnectedLoginMethodsResponse(
        providers.contains(AuthProvider.INTERNAL),
        providers.contains(AuthProvider.GOOGLE),
        providers.contains(AuthProvider.MICROSOFT));
  }

  private boolean hasProvider(AppUser user, CalendarProvider provider) {
    return !externalCalendarAccountRepository
        .findByUserAndProviderAndRevokedAtIsNull(user, provider)
        .isEmpty();
  }
}
