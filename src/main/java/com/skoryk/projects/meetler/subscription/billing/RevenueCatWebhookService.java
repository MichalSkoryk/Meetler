package com.skoryk.projects.meetler.subscription.billing;

import com.skoryk.projects.meetler.user.AppUser;
import com.skoryk.projects.meetler.user.AppUserRepository;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class RevenueCatWebhookService {

  private final RevenueCatWebhookVerifier verifier;
  private final ObjectMapper objectMapper;
  private final BillingWebhookEventRepository webhookEventRepository;
  private final AppUserRepository userRepository;
  private final RevenueCatReconciliationService reconciliationService;

  @Transactional
  public void process(byte[] rawBody, String authorization, String signature) {
    verifier.verify(rawBody, authorization, signature);
    JsonNode event = parseEvent(rawBody);
    String eventId = requiredText(event, "id");
    if (webhookEventRepository.existsByRevenueCatEventId(eventId)) {
      return;
    }

    for (AppUser user : resolveUsers(event)) {
      reconciliationService.synchronize(user);
    }

    webhookEventRepository.save(
        BillingWebhookEvent.builder()
            .revenueCatEventId(eventId)
            .appUserId(text(event, "app_user_id"))
            .eventType(requiredText(event, "type"))
            .environment(text(event, "environment"))
            .payload(new String(rawBody, StandardCharsets.UTF_8))
            .processedAt(OffsetDateTime.now(ZoneOffset.UTC))
            .build());
  }

  private JsonNode parseEvent(byte[] rawBody) {
    try {
      JsonNode event = objectMapper.readTree(rawBody).path("event");
      if (!event.isObject()) {
        throw new InvalidBillingWebhookException("Webhook event is missing");
      }
      return event;
    } catch (InvalidBillingWebhookException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new InvalidBillingWebhookException("Webhook payload is not valid JSON");
    }
  }

  private Set<AppUser> resolveUsers(JsonNode event) {
    Set<String> candidates = new LinkedHashSet<>();
    addText(candidates, event, "app_user_id");
    addText(candidates, event, "original_app_user_id");
    addArray(candidates, event, "aliases");
    addArray(candidates, event, "transferred_to");
    addArray(candidates, event, "redeemed_by");

    Set<AppUser> users = new LinkedHashSet<>();
    for (String candidate : candidates) {
      try {
        userRepository.findById(UUID.fromString(candidate)).ifPresent(users::add);
      } catch (IllegalArgumentException ignored) {
        // RevenueCat can also send anonymous IDs that do not belong to Meetler users.
      }
    }
    return users;
  }

  private void addText(Set<String> values, JsonNode node, String fieldName) {
    String value = text(node, fieldName);
    if (value != null) {
      values.add(value);
    }
  }

  private void addArray(Set<String> values, JsonNode node, String fieldName) {
    JsonNode array = node.path(fieldName);
    if (array.isArray()) {
      array.forEach(
          value -> {
            if (value.isTextual() && !value.asText().isBlank()) {
              values.add(value.asText());
            }
          });
    }
  }

  private String requiredText(JsonNode node, String fieldName) {
    String value = text(node, fieldName);
    if (value == null) {
      throw new InvalidBillingWebhookException("Webhook event is missing " + fieldName);
    }
    return value;
  }

  private String text(JsonNode node, String fieldName) {
    JsonNode value = node.get(fieldName);
    return value == null || value.isNull() || !value.isTextual() || value.asText().isBlank()
        ? null
        : value.asText();
  }
}
