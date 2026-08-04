package com.skoryk.projects.meetler.subscription;

import com.skoryk.projects.meetler.subscription.dto.UserSubscriptionResponse;
import com.skoryk.projects.meetler.user.AppUser;
import com.skoryk.projects.meetler.user.AppUserRepository;
import com.skoryk.projects.meetler.user.AppUserRole;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserSubscriptionService {

  private final UserSubscriptionRepository userSubscriptionRepository;
  private final SubscriptionPlanRepository subscriptionPlanRepository;
  private final AppUserRepository userRepository;
  private final UserSubscriptionMapper userSubscriptionMapper;

  public UserSubscriptionResponse getActiveSubscription(UUID userId, AppUser requester) {
    assertApplicationAdmin(requester);
    AppUser user = getUser(userId);
    UserSubscription subscription =
        userSubscriptionRepository
            .findFirstByUserAndStatusOrderByStartedAtDesc(user, SubscriptionStatus.ACTIVE)
            .orElseThrow(() -> new IllegalArgumentException("User has no active subscription"));

    return userSubscriptionMapper.toResponse(subscription);
  }

  @Transactional
  public UserSubscriptionResponse assignPlan(UUID userId, String planCode, AppUser requester) {
    assertApplicationAdmin(requester);
    AppUser user = getUser(userId);
    SubscriptionPlan plan =
        subscriptionPlanRepository
            .findByCodeAndActiveTrue(planCode)
            .orElseThrow(() -> new IllegalArgumentException("Subscription plan not found"));
    if (user.getRole() == AppUserRole.GUEST
        && !SubscriptionUsageService.DEFAULT_PLAN_CODE.equals(plan.getCode())) {
      throw new IllegalArgumentException("Guest accounts must remain on the FREE plan");
    }

    OffsetDateTime now = OffsetDateTime.now();
    userSubscriptionRepository
        .findFirstByUserAndStatusOrderByStartedAtDesc(user, SubscriptionStatus.ACTIVE)
        .ifPresent(
            current -> {
              if (Objects.equals(current.getPlan().getId(), plan.getId())) {
                throw new IllegalArgumentException("User already has this subscription plan");
              }
              current.setStatus(SubscriptionStatus.CANCELLED);
              current.setExpiresAt(now);
              userSubscriptionRepository.saveAndFlush(current);
            });

    UserSubscription subscription =
        UserSubscription.builder()
            .user(user)
            .plan(plan)
            .status(SubscriptionStatus.ACTIVE)
            .startedAt(now)
            .build();

    return userSubscriptionMapper.toResponse(userSubscriptionRepository.save(subscription));
  }

  private AppUser getUser(UUID userId) {
    return userRepository
        .findById(userId)
        .orElseThrow(() -> new IllegalArgumentException("User not found"));
  }

  private void assertApplicationAdmin(AppUser requester) {
    if (requester == null || requester.getRole() != AppUserRole.ADMIN) {
      throw new IllegalArgumentException("Only application admins can manage subscriptions");
    }
  }
}
