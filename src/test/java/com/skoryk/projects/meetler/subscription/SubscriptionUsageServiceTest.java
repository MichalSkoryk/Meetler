package com.skoryk.projects.meetler.subscription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.skoryk.projects.meetler.availability.repository.AvailabilityTemplateRepository;
import com.skoryk.projects.meetler.group.member.GroupMemberRepository;
import com.skoryk.projects.meetler.group.member.GroupRole;
import com.skoryk.projects.meetler.user.AppUser;
import com.skoryk.projects.meetler.user.AppUserRole;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SubscriptionUsageServiceTest {

  @Mock private SubscriptionPlanRepository planRepository;
  @Mock private UserSubscriptionRepository userSubscriptionRepository;
  @Mock private GroupMemberRepository groupMemberRepository;
  @Mock private AvailabilityTemplateRepository availabilityTemplateRepository;

  @InjectMocks private SubscriptionUsageService service;

  @Test
  void activePlanForReturnsUsersActiveSubscriptionPlan() {
    AppUser user = user();
    SubscriptionPlan plus = plan("PLUS", 5, 5);
    UserSubscription subscription = subscription(user, plus);
    when(userSubscriptionRepository.findFirstByUserAndStatusOrderByStartedAtDesc(
            user, SubscriptionStatus.ACTIVE))
        .thenReturn(Optional.of(subscription));

    SubscriptionPlan result = service.activePlanFor(user);

    assertThat(result).isSameAs(plus);
  }

  @Test
  void activePlanForFallsBackToFreePlanWhenUserHasNoActiveSubscription() {
    AppUser user = user();
    SubscriptionPlan free = plan("FREE", 1, 1);
    when(userSubscriptionRepository.findFirstByUserAndStatusOrderByStartedAtDesc(
            user, SubscriptionStatus.ACTIVE))
        .thenReturn(Optional.empty());
    when(planRepository.findByCodeAndActiveTrue(SubscriptionUsageService.DEFAULT_PLAN_CODE))
        .thenReturn(Optional.of(free));

    SubscriptionPlan result = service.activePlanFor(user);

    assertThat(result).isSameAs(free);
  }

  @Test
  void activePlanForFailsClearlyWhenFreePlanIsMissing() {
    AppUser user = user();
    when(userSubscriptionRepository.findFirstByUserAndStatusOrderByStartedAtDesc(
            user, SubscriptionStatus.ACTIVE))
        .thenReturn(Optional.empty());
    when(planRepository.findByCodeAndActiveTrue(SubscriptionUsageService.DEFAULT_PLAN_CODE))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.activePlanFor(user))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("No FREE plan found");
  }

  @Test
  void getUsageReturnsPlanLimitsAndCurrentUsageCounts() {
    AppUser user = user();
    SubscriptionPlan plan = plan("PLUS", 5, 5);
    activePlan(user, plan);
    when(groupMemberRepository.countByUserAndRole(user, GroupRole.OWNER)).thenReturn(2L);
    when(availabilityTemplateRepository.countByUser(user)).thenReturn(3L);

    SubscriptionUsageResponse response = service.getUsage(user);

    assertThat(response.getPlanCode()).isEqualTo("PLUS");
    assertThat(response.getPlanName()).isEqualTo("PLUS");
    assertThat(response.getOwnedGroupsUsed()).isEqualTo(2);
    assertThat(response.getMaxOwnedGroups()).isEqualTo(5);
    assertThat(response.getAvailabilityTemplatesUsed()).isEqualTo(3);
    assertThat(response.getMaxAvailabilityTemplates()).isEqualTo(5);
    verify(groupMemberRepository).countByUserAndRole(user, GroupRole.OWNER);
    verify(availabilityTemplateRepository).countByUser(user);
  }

  private void activePlan(AppUser user, SubscriptionPlan plan) {
    when(userSubscriptionRepository.findFirstByUserAndStatusOrderByStartedAtDesc(
            user, SubscriptionStatus.ACTIVE))
        .thenReturn(Optional.of(subscription(user, plan)));
  }

  private UserSubscription subscription(AppUser user, SubscriptionPlan plan) {
    return UserSubscription.builder()
        .id(UUID.randomUUID())
        .user(user)
        .plan(plan)
        .status(SubscriptionStatus.ACTIVE)
        .startedAt(OffsetDateTime.now().minusDays(1))
        .build();
  }

  private SubscriptionPlan plan(String code, int maxOwnedGroups, int maxAvailabilityTemplates) {
    return SubscriptionPlan.builder()
        .id(UUID.randomUUID())
        .code(code)
        .name(code)
        .maxOwnedGroups(maxOwnedGroups)
        .maxAvailabilityTemplates(maxAvailabilityTemplates)
        .active(true)
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
