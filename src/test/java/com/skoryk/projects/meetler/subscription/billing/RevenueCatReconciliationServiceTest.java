package com.skoryk.projects.meetler.subscription.billing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.skoryk.projects.meetler.user.AppUser;
import com.skoryk.projects.meetler.user.AppUserRole;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RevenueCatReconciliationServiceTest {

  @Mock private BillingProperties properties;
  @Mock private RevenueCatClient revenueCatClient;
  @Mock private BillingCustomerRepository customerRepository;
  @Mock private BillingEntitlementRepository entitlementRepository;
  @InjectMocks private RevenueCatReconciliationService service;

  private final AppUser user =
      AppUser.builder()
          .id(UUID.randomUUID())
          .email("paid@example.com")
          .role(AppUserRole.USER)
          .build();

  @BeforeEach
  void setUp() {
    when(properties.isConfigured()).thenReturn(true);
    when(customerRepository.findByUser(user)).thenReturn(Optional.empty());
    when(customerRepository.save(any(BillingCustomer.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(entitlementRepository.findByCustomer(any(BillingCustomer.class))).thenReturn(List.of());
  }

  @Test
  void storesActiveSandboxEntitlement() {
    OffsetDateTime now = OffsetDateTime.now();
    when(properties.planCodeFor("plus")).thenReturn("PLUS");
    when(revenueCatClient.fetchSubscriber(user.getId()))
        .thenReturn(
            new RevenueCatSubscriberSnapshot(
                user.getId().toString(),
                "https://pay.example.test/manage",
                List.of(
                    new RevenueCatSubscriberSnapshot.Entitlement(
                        "plus",
                        "plus_monthly",
                        "PADDLE",
                        BillingEnvironment.SANDBOX,
                        now.minusDays(1),
                        now.plusDays(29),
                        null,
                        null,
                        null,
                        null))));

    service.synchronize(user);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<Iterable<BillingEntitlement>> captor = ArgumentCaptor.forClass(Iterable.class);
    verify(entitlementRepository).saveAll(captor.capture());
    BillingEntitlement saved = captor.getValue().iterator().next();
    assertThat(saved.getPlanCode()).isEqualTo("PLUS");
    assertThat(saved.getStatus()).isEqualTo(BillingEntitlementStatus.ACTIVE);
    assertThat(saved.getEnvironment()).isEqualTo(BillingEnvironment.SANDBOX);
    assertThat(saved.isWillRenew()).isTrue();
    verify(customerRepository)
        .save(
            org.mockito.ArgumentMatchers.argThat(
                customer -> "https://pay.example.test/manage".equals(customer.getManagementUrl())));
  }

  @Test
  void refundedEntitlementIsExpired() {
    OffsetDateTime now = OffsetDateTime.now();
    when(properties.planCodeFor("pro")).thenReturn("PRO");
    when(revenueCatClient.fetchSubscriber(user.getId()))
        .thenReturn(
            new RevenueCatSubscriberSnapshot(
                user.getId().toString(),
                null,
                List.of(
                    new RevenueCatSubscriberSnapshot.Entitlement(
                        "pro",
                        "pro_annual",
                        "APP_STORE",
                        BillingEnvironment.PRODUCTION,
                        now.minusDays(20),
                        now.plusDays(300),
                        null,
                        null,
                        null,
                        now.minusMinutes(1)))));

    service.synchronize(user);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<Iterable<BillingEntitlement>> captor = ArgumentCaptor.forClass(Iterable.class);
    verify(entitlementRepository).saveAll(captor.capture());
    BillingEntitlement saved = captor.getValue().iterator().next();
    assertThat(saved.getStatus()).isEqualTo(BillingEntitlementStatus.EXPIRED);
    assertThat(saved.isWillRenew()).isFalse();
  }
}
