package com.skoryk.projects.meetler.subscription;

import com.skoryk.projects.meetler.availability.repository.AvailabilityTemplateRepository;
import com.skoryk.projects.meetler.group.member.GroupMemberRepository;
import com.skoryk.projects.meetler.group.member.GroupRole;
import com.skoryk.projects.meetler.subscription.billing.BillingCustomer;
import com.skoryk.projects.meetler.subscription.billing.BillingCustomerRepository;
import com.skoryk.projects.meetler.subscription.billing.BillingEntitlement;
import com.skoryk.projects.meetler.subscription.billing.BillingEntitlementRepository;
import com.skoryk.projects.meetler.subscription.billing.BillingProperties;
import com.skoryk.projects.meetler.user.AppUser;
import com.skoryk.projects.meetler.user.AppUserRole;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
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
  private final BillingProperties billingProperties;
  private final BillingCustomerRepository billingCustomerRepository;
  private final BillingEntitlementRepository billingEntitlementRepository;

  public SubscriptionPlan activePlanFor(AppUser user) {
    if (user.getRole() == AppUserRole.GUEST) {
      return freePlan();
    }

    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
    Optional<UserSubscription> userSubscription =
        userSubscriptionRepository.findFirstByUserAndStatusOrderByStartedAtDesc(
            user, SubscriptionStatus.ACTIVE);
    Optional<SubscriptionPlan> assignedPlan =
        userSubscription
            .filter(
                subscription ->
                    subscription.getExpiresAt() == null || subscription.getExpiresAt().isAfter(now))
            .map(UserSubscription::getPlan);
    Optional<String> billedPlanCode =
        effectiveBillingEntitlements(user, now).stream()
            .map(BillingEntitlement::getPlanCode)
            .max(Comparator.comparingInt(this::planRank));

    String assignedPlanCode = assignedPlan.map(SubscriptionPlan::getCode).orElse(DEFAULT_PLAN_CODE);
    String selectedPlanCode =
        billedPlanCode
            .filter(code -> planRank(code) > planRank(assignedPlanCode))
            .orElse(assignedPlanCode);

    if (assignedPlan.isPresent() && assignedPlanCode.equals(selectedPlanCode)) {
      return assignedPlan.get();
    }
    if (DEFAULT_PLAN_CODE.equals(selectedPlanCode)) {
      return freePlan();
    }
    return planRepository
        .findByCodeAndActiveTrue(selectedPlanCode)
        .orElseThrow(
            () -> new IllegalStateException("No active " + selectedPlanCode + " plan found"));
  }

  public SubscriptionUsageResponse getUsage(AppUser user) {
    SubscriptionPlan subscriptionPlan = activePlanFor(user);
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
    List<BillingEntitlement> effectiveEntitlements =
        user.getRole() == AppUserRole.GUEST ? List.of() : effectiveBillingEntitlements(user, now);
    BillingEntitlement primaryEntitlement =
        effectiveEntitlements.stream()
            .max(Comparator.comparingInt(entitlement -> planRank(entitlement.getPlanCode())))
            .orElse(null);
    BillingEntitlement planEntitlement =
        primaryEntitlement != null
                && primaryEntitlement.getPlanCode().equalsIgnoreCase(subscriptionPlan.getCode())
            ? primaryEntitlement
            : null;
    BillingCustomer customer = billingCustomerRepository.findByUser(user).orElse(null);
    boolean guest = user.getRole() == AppUserRole.GUEST;
    boolean billingConfigured = billingProperties.isConfigured();

    return SubscriptionUsageResponse.builder()
        .planCode(subscriptionPlan.getCode())
        .planName(subscriptionPlan.getName())
        .ownedGroupsUsed(getOwnedGroups(user))
        .maxOwnedGroups(subscriptionPlan.getMaxOwnedGroups())
        .availabilityTemplatesUsed(getAvailabilityTemplates(user))
        .maxAvailabilityTemplates(subscriptionPlan.getMaxAvailabilityTemplates())
        .billingConfigured(billingConfigured)
        .canPurchase(billingConfigured && !guest)
        .upgradeRequiresAccount(guest)
        .billingStatus(billingStatus(subscriptionPlan, planEntitlement, billingConfigured))
        .store(planEntitlement == null ? null : planEntitlement.getStore())
        .willRenew(planEntitlement == null ? null : planEntitlement.isWillRenew())
        .renewsOrExpiresAt(accessEndsAt(planEntitlement, now))
        .managementUrl(customer == null ? null : customer.getManagementUrl())
        .build();
  }

  private SubscriptionPlan freePlan() {
    return planRepository
        .findByCodeAndActiveTrue(DEFAULT_PLAN_CODE)
        .orElseThrow(() -> new IllegalStateException("No FREE plan found"));
  }

  private List<BillingEntitlement> effectiveBillingEntitlements(AppUser user, OffsetDateTime now) {
    return billingEntitlementRepository.findByCustomerUser(user).stream()
        .filter(entitlement -> entitlement.isEffectiveAt(now, billingProperties.getMode()))
        .toList();
  }

  private String billingStatus(
      SubscriptionPlan plan, BillingEntitlement entitlement, boolean billingConfigured) {
    if (entitlement != null) {
      return entitlement.getStatus().name();
    }
    if (!DEFAULT_PLAN_CODE.equals(plan.getCode())) {
      return "MANUAL";
    }
    return billingConfigured ? "FREE" : "DISABLED";
  }

  private OffsetDateTime accessEndsAt(BillingEntitlement entitlement, OffsetDateTime now) {
    if (entitlement == null) {
      return null;
    }
    if (entitlement.getGracePeriodExpiresAt() != null
        && entitlement.getGracePeriodExpiresAt().isAfter(now)) {
      return entitlement.getGracePeriodExpiresAt();
    }
    return entitlement.getExpiresAt();
  }

  private int planRank(String planCode) {
    return switch (planCode) {
      case "PRO" -> 2;
      case "PLUS" -> 1;
      default -> 0;
    };
  }

  private int getOwnedGroups(AppUser user) {
    return Math.toIntExact(groupMemberRepository.countByUserAndRole(user, GroupRole.OWNER));
  }

  private int getAvailabilityTemplates(AppUser user) {
    return Math.toIntExact(availabilityTemplateRepository.countByUser(user));
  }
}
