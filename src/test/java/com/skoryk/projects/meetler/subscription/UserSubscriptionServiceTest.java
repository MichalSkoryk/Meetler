package com.skoryk.projects.meetler.subscription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.skoryk.projects.meetler.subscription.dto.UserSubscriptionResponse;
import com.skoryk.projects.meetler.user.AppUser;
import com.skoryk.projects.meetler.user.AppUserRepository;
import com.skoryk.projects.meetler.user.AppUserRole;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserSubscriptionServiceTest {

  @Mock private UserSubscriptionRepository userSubscriptionRepository;
  @Mock private SubscriptionPlanRepository subscriptionPlanRepository;
  @Mock private AppUserRepository userRepository;

  @Spy
  private UserSubscriptionMapper userSubscriptionMapper =
      Mappers.getMapper(UserSubscriptionMapper.class);

  @InjectMocks private UserSubscriptionService service;

  @Test
  void assignPlanRequiresApplicationAdmin() {
    AppUser requester = user(AppUserRole.USER);

    assertThatThrownBy(() -> service.assignPlan(UUID.randomUUID(), "PLUS", requester))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Only application admins can manage subscriptions");

    verify(userSubscriptionRepository, never()).save(any());
  }

  @Test
  void assignPlanCancelsCurrentSubscriptionAndCreatesNewActiveSubscription() {
    AppUser admin = user(AppUserRole.ADMIN);
    AppUser target = user(AppUserRole.USER);
    SubscriptionPlan free = plan("FREE");
    SubscriptionPlan plus = plan("PLUS");
    UserSubscription current = subscription(target, free, SubscriptionStatus.ACTIVE);

    when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));
    when(subscriptionPlanRepository.findByCodeAndActiveTrue("PLUS")).thenReturn(Optional.of(plus));
    when(userSubscriptionRepository.findFirstByUserAndStatusOrderByStartedAtDesc(
            target, SubscriptionStatus.ACTIVE))
        .thenReturn(Optional.of(current));
    when(userSubscriptionRepository.save(any(UserSubscription.class)))
        .thenAnswer(
            invocation -> {
              UserSubscription saved = invocation.getArgument(0);
              saved.setId(UUID.randomUUID());
              return saved;
            });

    UserSubscriptionResponse response = service.assignPlan(target.getId(), "PLUS", admin);

    assertThat(current.getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);
    assertThat(current.getExpiresAt()).isNotNull();
    verify(userSubscriptionRepository).saveAndFlush(current);

    ArgumentCaptor<UserSubscription> newSubscriptionCaptor =
        ArgumentCaptor.forClass(UserSubscription.class);
    verify(userSubscriptionRepository).save(newSubscriptionCaptor.capture());
    UserSubscription newSubscription = newSubscriptionCaptor.getValue();
    assertThat(newSubscription.getUser()).isSameAs(target);
    assertThat(newSubscription.getPlan()).isSameAs(plus);
    assertThat(newSubscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
    assertThat(newSubscription.getStartedAt()).isNotNull();
    assertThat(response.getPlanCode()).isEqualTo("PLUS");
    assertThat(response.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
  }

  @Test
  void assignPlanCreatesActiveSubscriptionWhenUserHadNoSubscription() {
    AppUser admin = user(AppUserRole.ADMIN);
    AppUser target = user(AppUserRole.USER);
    SubscriptionPlan free = plan("FREE");
    when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));
    when(subscriptionPlanRepository.findByCodeAndActiveTrue("FREE")).thenReturn(Optional.of(free));
    when(userSubscriptionRepository.findFirstByUserAndStatusOrderByStartedAtDesc(
            target, SubscriptionStatus.ACTIVE))
        .thenReturn(Optional.empty());
    when(userSubscriptionRepository.save(any(UserSubscription.class)))
        .thenAnswer(
            invocation -> {
              UserSubscription saved = invocation.getArgument(0);
              saved.setId(UUID.randomUUID());
              return saved;
            });

    UserSubscriptionResponse response = service.assignPlan(target.getId(), "FREE", admin);

    verify(userSubscriptionRepository, never()).saveAndFlush(any());
    assertThat(response.getPlanCode()).isEqualTo("FREE");
    assertThat(response.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
  }

  @Test
  void assignPlanRejectsSamePlan() {
    AppUser admin = user(AppUserRole.ADMIN);
    AppUser target = user(AppUserRole.USER);
    SubscriptionPlan free = plan("FREE");
    UserSubscription current = subscription(target, free, SubscriptionStatus.ACTIVE);
    when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));
    when(subscriptionPlanRepository.findByCodeAndActiveTrue("FREE")).thenReturn(Optional.of(free));
    when(userSubscriptionRepository.findFirstByUserAndStatusOrderByStartedAtDesc(
            target, SubscriptionStatus.ACTIVE))
        .thenReturn(Optional.of(current));

    assertThatThrownBy(() -> service.assignPlan(target.getId(), "FREE", admin))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("User already has this subscription plan");

    verify(userSubscriptionRepository, never()).save(any());
    verify(userSubscriptionRepository, never()).saveAndFlush(any());
  }

  @Test
  void getActiveSubscriptionReturnsCurrentSubscriptionForAdmin() {
    AppUser admin = user(AppUserRole.ADMIN);
    AppUser target = user(AppUserRole.USER);
    SubscriptionPlan plus = plan("PLUS");
    UserSubscription subscription = subscription(target, plus, SubscriptionStatus.ACTIVE);
    when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));
    when(userSubscriptionRepository.findFirstByUserAndStatusOrderByStartedAtDesc(
            target, SubscriptionStatus.ACTIVE))
        .thenReturn(Optional.of(subscription));

    UserSubscriptionResponse response = service.getActiveSubscription(target.getId(), admin);

    assertThat(response.getPlanCode()).isEqualTo("PLUS");
    assertThat(response.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
  }

  private UserSubscription subscription(
      AppUser user, SubscriptionPlan plan, SubscriptionStatus status) {
    return UserSubscription.builder()
        .id(UUID.randomUUID())
        .user(user)
        .plan(plan)
        .status(status)
        .startedAt(OffsetDateTime.now().minusDays(1))
        .build();
  }

  private SubscriptionPlan plan(String code) {
    return SubscriptionPlan.builder()
        .id(UUID.randomUUID())
        .code(code)
        .name(code)
        .maxOwnedGroups(1)
        .maxAvailabilityTemplates(1)
        .active(true)
        .build();
  }

  private AppUser user(AppUserRole role) {
    return AppUser.builder()
        .id(UUID.randomUUID())
        .email(UUID.randomUUID() + "@example.com")
        .role(role)
        .build();
  }
}
