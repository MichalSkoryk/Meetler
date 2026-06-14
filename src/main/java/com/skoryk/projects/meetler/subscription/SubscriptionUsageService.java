package com.skoryk.projects.meetler.subscription;

import com.skoryk.projects.meetler.availability.repository.AvailabilityTemplateRepository;
import com.skoryk.projects.meetler.group.member.GroupMemberRepository;
import com.skoryk.projects.meetler.group.member.GroupRole;
import com.skoryk.projects.meetler.user.AppUser;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SubscriptionUsageService {

  public static final String DEFAULT_PLAN_CODE = "FREE";

  private final SubscriptionPlanRepository planRepository;
  private final UserSubscriptionRepository userSubscriptionRepository;
  private final GroupMemberRepository groupMemberRepository;
  private final AvailabilityTemplateRepository availabilityTemplateRepository;

  public SubscriptionPlan activePlanFor(AppUser user) {
    Optional<UserSubscription> userSubscription =
        userSubscriptionRepository.findFirstByUserAndStatusOrderByStartedAtDesc(
            user, SubscriptionStatus.ACTIVE);

    if (userSubscription.isPresent()) {
      return userSubscription.get().getPlan();
    }
    return planRepository
        .findByCodeAndActiveTrue(DEFAULT_PLAN_CODE)
        .orElseThrow(() -> new IllegalStateException("No FREE plan found"));
  }

  public SubscriptionUsageResponse getUsage(AppUser user) {
    SubscriptionPlan subscriptionPlan = activePlanFor(user);

    return SubscriptionUsageResponse.builder()
        .planCode(subscriptionPlan.getCode())
        .planName(subscriptionPlan.getName())
        .ownedGroupsUsed(getOwnedGroups(user))
        .maxOwnedGroups(subscriptionPlan.getMaxOwnedGroups())
        .availabilityTemplatesUsed(getAvailabilityTemplates(user))
        .maxAvailabilityTemplates(subscriptionPlan.getMaxAvailabilityTemplates())
        .build();
  }

  private int getOwnedGroups(AppUser user) {
    return Math.toIntExact(groupMemberRepository.countByUserAndRole(user, GroupRole.OWNER));
  }

  private int getAvailabilityTemplates(AppUser user) {
    return Math.toIntExact(availabilityTemplateRepository.countByUser(user));
  }
}
