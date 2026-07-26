package com.skoryk.projects.meetler.subscription.billing;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

@Component
public class RevenueCatClient {

  private final BillingProperties properties;

  public RevenueCatClient(BillingProperties properties) {
    this.properties = properties;
  }

  public RevenueCatSubscriberSnapshot fetchSubscriber(UUID userId) {
    if (!properties.isConfigured()) {
      throw new BillingUnavailableException("Billing is not configured");
    }

    JsonNode response =
        RestClient.builder()
            .baseUrl(properties.getApiUrl())
            .defaultHeader(
                HttpHeaders.AUTHORIZATION, "Bearer " + properties.getSecretApiKey().trim())
            .build()
            .get()
            .uri("/subscribers/{appUserId}", userId)
            .retrieve()
            .body(JsonNode.class);

    if (response == null || !response.path("subscriber").isObject()) {
      throw new IllegalStateException("RevenueCat returned an invalid subscriber response");
    }

    JsonNode subscriber = response.path("subscriber");
    JsonNode subscriptions = subscriber.path("subscriptions");
    List<RevenueCatSubscriberSnapshot.Entitlement> entitlements = new ArrayList<>();
    Iterator<Map.Entry<String, JsonNode>> fields =
        subscriber.path("entitlements").properties().iterator();

    while (fields.hasNext()) {
      Map.Entry<String, JsonNode> field = fields.next();
      JsonNode entitlement = field.getValue();
      String productId = text(entitlement, "product_identifier");
      JsonNode subscription = productId == null ? null : subscriptions.path(productId);
      boolean hasSubscription = subscription != null && subscription.isObject();

      entitlements.add(
          new RevenueCatSubscriberSnapshot.Entitlement(
              field.getKey(),
              productId == null ? "unknown" : productId,
              normalizeStore(hasSubscription ? text(subscription, "store") : null),
              hasSubscription && subscription.path("is_sandbox").asBoolean(false)
                  ? BillingEnvironment.SANDBOX
                  : BillingEnvironment.PRODUCTION,
              firstDate(subscription, entitlement, "purchase_date"),
              firstDate(subscription, entitlement, "expires_date"),
              firstDate(subscription, entitlement, "grace_period_expires_date"),
              date(subscription, "unsubscribe_detected_at"),
              date(subscription, "billing_issues_detected_at"),
              date(subscription, "refunded_at")));
    }

    return new RevenueCatSubscriberSnapshot(
        text(subscriber, "original_app_user_id"),
        text(subscriber, "management_url"),
        List.copyOf(entitlements));
  }

  private OffsetDateTime firstDate(JsonNode preferred, JsonNode fallback, String fieldName) {
    OffsetDateTime value = date(preferred, fieldName);
    return value == null ? date(fallback, fieldName) : value;
  }

  private OffsetDateTime date(JsonNode node, String fieldName) {
    String value = text(node, fieldName);
    if (value == null) {
      return null;
    }
    try {
      return OffsetDateTime.parse(value);
    } catch (RuntimeException ex) {
      return null;
    }
  }

  private String text(JsonNode node, String fieldName) {
    if (node == null || !node.isObject()) {
      return null;
    }
    JsonNode value = node.get(fieldName);
    return value == null || value.isNull() || !value.isTextual() || value.asText().isBlank()
        ? null
        : value.asText();
  }

  private String normalizeStore(String store) {
    return store == null ? null : store.toUpperCase(Locale.ROOT);
  }
}
