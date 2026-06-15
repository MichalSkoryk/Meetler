package com.skoryk.projects.meetler.me;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.skoryk.projects.meetler.calendar.CalendarProvider;
import com.skoryk.projects.meetler.calendar.external.ExternalCalendarAccount;
import com.skoryk.projects.meetler.calendar.external.ExternalCalendarAccountRepository;
import com.skoryk.projects.meetler.subscription.SubscriptionUsageResponse;
import com.skoryk.projects.meetler.subscription.SubscriptionUsageService;
import com.skoryk.projects.meetler.user.AppUser;
import com.skoryk.projects.meetler.user.AppUserRole;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MeServiceTest {

  @Mock private SubscriptionUsageService subscriptionUsageService;
  @Mock private ExternalCalendarAccountRepository externalCalendarAccountRepository;

  @InjectMocks private MeService service;

  @Test
  void getBootstrapReturnsUserUsageAndConnectedCalendarFlags() {
    AppUser user = user();
    SubscriptionUsageResponse usage =
        SubscriptionUsageResponse.builder()
            .planCode("FREE")
            .planName("Free")
            .ownedGroupsUsed(1)
            .maxOwnedGroups(2)
            .availabilityTemplatesUsed(3)
            .maxAvailabilityTemplates(4)
            .build();
    when(subscriptionUsageService.getUsage(user)).thenReturn(usage);
    when(externalCalendarAccountRepository.findByUserAndProviderAndRevokedAtIsNull(
            user, CalendarProvider.GOOGLE))
        .thenReturn(List.of(account()));
    when(externalCalendarAccountRepository.findByUserAndProviderAndRevokedAtIsNull(
            user, CalendarProvider.TEAMS))
        .thenReturn(List.of());

    MeBootstrapResponse response = service.getBootstrap(user);

    assertThat(response.getUser().getId()).isEqualTo(user.getId());
    assertThat(response.getUser().getEmail()).isEqualTo(user.getEmail());
    assertThat(response.getUser().getName()).isEqualTo(user.getName());
    assertThat(response.getUser().getRole()).isEqualTo(AppUserRole.USER);
    assertThat(response.getUser().isHasPassword()).isTrue();
    assertThat(response.getSubscription()).isSameAs(usage);
    assertThat(response.getConnectedCalendars().isGoogle()).isTrue();
    assertThat(response.getConnectedCalendars().isMicrosoft()).isFalse();
  }

  @Test
  void getSubscriptionUsageDelegatesToSubscriptionUsageService() {
    AppUser user = user();
    SubscriptionUsageResponse usage = SubscriptionUsageResponse.builder().planCode("FREE").build();
    when(subscriptionUsageService.getUsage(user)).thenReturn(usage);

    SubscriptionUsageResponse response = service.getSubscriptionUsage(user);

    assertThat(response).isSameAs(usage);
    verify(subscriptionUsageService).getUsage(user);
  }

  private ExternalCalendarAccount account() {
    return ExternalCalendarAccount.builder().id(UUID.randomUUID()).build();
  }

  private AppUser user() {
    return AppUser.builder()
        .id(UUID.randomUUID())
        .email("me@example.com")
        .name("Meetler User")
        .role(AppUserRole.USER)
        .passwordHash("hash")
        .build();
  }
}
