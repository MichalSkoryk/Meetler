package com.skoryk.projects.meetler.subscription;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.skoryk.projects.meetler.user.AppUser;
import com.skoryk.projects.meetler.user.AppUserRole;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SubscriptionLimitServiceTest {

  @Mock private SubscriptionUsageService subscriptionUsageService;

  @InjectMocks private SubscriptionLimitService service;

  @Test
  void assertCanCreateGroupAllowsCreationBelowOwnedGroupLimit() {
    AppUser user = user();
    when(subscriptionUsageService.getUsage(user)).thenReturn(usage(1, 2, 1, 1));

    assertThatCode(() -> service.assertCanCreateGroup(user)).doesNotThrowAnyException();
  }

  @Test
  void assertCanCreateGroupRejectsCreationAtOwnedGroupLimit() {
    AppUser user = user();
    when(subscriptionUsageService.getUsage(user)).thenReturn(usage(1, 1, 1, 1));

    assertThatThrownBy(() -> service.assertCanCreateGroup(user))
        .isInstanceOf(SubscriptionLimitExceededException.class)
        .hasMessage("Limit of created groups was reached");
  }

  @Test
  void assertCanCreateAvailabilityTemplateAllowsCreationBelowTemplateLimit() {
    AppUser user = user();
    when(subscriptionUsageService.getUsage(user)).thenReturn(usage(1, 1, 1, 2));

    assertThatCode(() -> service.assertCanCreateAvailabilityTemplate(user))
        .doesNotThrowAnyException();
  }

  @Test
  void assertCanCreateAvailabilityTemplateRejectsCreationAtTemplateLimit() {
    AppUser user = user();
    when(subscriptionUsageService.getUsage(user)).thenReturn(usage(1, 1, 1, 1));

    assertThatThrownBy(() -> service.assertCanCreateAvailabilityTemplate(user))
        .isInstanceOf(SubscriptionLimitExceededException.class)
        .hasMessage("Limit of created availability templates was reached");
  }

  private SubscriptionUsageResponse usage(
      int ownedGroupsUsed,
      int maxOwnedGroups,
      int availabilityTemplatesUsed,
      int maxAvailabilityTemplates) {
    return SubscriptionUsageResponse.builder()
        .planCode("FREE")
        .planName("Free")
        .ownedGroupsUsed(ownedGroupsUsed)
        .maxOwnedGroups(maxOwnedGroups)
        .availabilityTemplatesUsed(availabilityTemplatesUsed)
        .maxAvailabilityTemplates(maxAvailabilityTemplates)
        .build();
  }

  private AppUser user() {
    return AppUser.builder()
        .id(UUID.randomUUID())
        .email(UUID.randomUUID() + "@example.com")
        .role(AppUserRole.USER)
        .build();
  }
}
