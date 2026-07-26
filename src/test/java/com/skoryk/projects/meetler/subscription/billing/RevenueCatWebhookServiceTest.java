package com.skoryk.projects.meetler.subscription.billing;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.skoryk.projects.meetler.user.AppUser;
import com.skoryk.projects.meetler.user.AppUserRepository;
import com.skoryk.projects.meetler.user.AppUserRole;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class RevenueCatWebhookServiceTest {

  @Mock private RevenueCatWebhookVerifier verifier;
  @Spy private ObjectMapper objectMapper = new ObjectMapper();
  @Mock private BillingWebhookEventRepository webhookEventRepository;
  @Mock private AppUserRepository userRepository;
  @Mock private RevenueCatReconciliationService reconciliationService;
  @InjectMocks private RevenueCatWebhookService service;

  private UUID userId;
  private AppUser user;

  @BeforeEach
  void setUp() {
    userId = UUID.randomUUID();
    user = AppUser.builder().id(userId).email("webhook@example.com").role(AppUserRole.USER).build();
  }

  @Test
  void reconcilesAliasAndStoresProcessedEvent() {
    byte[] payload =
        ("{\"api_version\":\"1.0\",\"event\":{"
                + "\"id\":\"event-1\",\"type\":\"RENEWAL\","
                + "\"app_user_id\":\"anonymous\",\"aliases\":[\""
                + userId
                + "\"],\"environment\":\"SANDBOX\"}}")
            .getBytes(StandardCharsets.UTF_8);
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));

    service.process(payload, "Bearer secret", "t=1,v1=abc");

    verify(verifier).verify(payload, "Bearer secret", "t=1,v1=abc");
    verify(reconciliationService).synchronize(user);
    verify(webhookEventRepository).save(any(BillingWebhookEvent.class));
  }

  @Test
  void duplicateEventIsAcknowledgedWithoutAnotherReconciliation() {
    byte[] payload =
        "{\"event\":{\"id\":\"event-1\",\"type\":\"RENEWAL\"}}".getBytes(StandardCharsets.UTF_8);
    when(webhookEventRepository.existsByRevenueCatEventId("event-1")).thenReturn(true);

    service.process(payload, "Bearer secret", "t=1,v1=abc");

    verify(reconciliationService, never()).synchronize(any());
    verify(webhookEventRepository, never()).save(any());
  }
}
