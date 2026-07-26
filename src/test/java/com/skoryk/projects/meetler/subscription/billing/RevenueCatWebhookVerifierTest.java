package com.skoryk.projects.meetler.subscription.billing;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RevenueCatWebhookVerifierTest {

  private static final Instant NOW = Instant.parse("2026-07-21T10:00:00Z");
  private final BillingProperties properties = new BillingProperties();
  private RevenueCatWebhookVerifier verifier;

  @BeforeEach
  void setUp() {
    properties.setMode(BillingMode.SANDBOX);
    properties.setSecretApiKey("secret-api-key");
    properties.setWebhookAuthorization("Bearer webhook-secret");
    properties.setWebhookSigningSecret("signing-secret");
    verifier = new RevenueCatWebhookVerifier(properties, Clock.fixed(NOW, ZoneOffset.UTC));
  }

  @Test
  void acceptsCurrentCorrectlySignedPayload() {
    byte[] body = "{\"event\":{\"id\":\"event-1\"}}".getBytes(StandardCharsets.UTF_8);
    String signature = signature(NOW.getEpochSecond(), body);

    assertThatCode(() -> verifier.verify(body, "Bearer webhook-secret", signature))
        .doesNotThrowAnyException();
  }

  @Test
  void rejectsInvalidAuthorization() {
    byte[] body = "{}".getBytes(StandardCharsets.UTF_8);

    assertThatThrownBy(() -> verifier.verify(body, "wrong", signature(NOW.getEpochSecond(), body)))
        .isInstanceOf(InvalidBillingWebhookException.class)
        .hasMessage("Invalid webhook authorization");
  }

  @Test
  void rejectsReplayedPayload() {
    byte[] body = "{}".getBytes(StandardCharsets.UTF_8);
    long oldTimestamp = NOW.minusSeconds(301).getEpochSecond();

    assertThatThrownBy(
            () -> verifier.verify(body, "Bearer webhook-secret", signature(oldTimestamp, body)))
        .isInstanceOf(InvalidBillingWebhookException.class)
        .hasMessageContaining("outside tolerance");
  }

  @Test
  void rejectsChangedPayload() {
    byte[] signedBody = "{}".getBytes(StandardCharsets.UTF_8);
    byte[] changedBody = "{ }".getBytes(StandardCharsets.UTF_8);

    assertThatThrownBy(
            () ->
                verifier.verify(
                    changedBody,
                    "Bearer webhook-secret",
                    signature(NOW.getEpochSecond(), signedBody)))
        .isInstanceOf(InvalidBillingWebhookException.class)
        .hasMessage("Invalid webhook signature");
  }

  private String signature(long timestamp, byte[] body) {
    try {
      String signed = timestamp + "." + new String(body, StandardCharsets.UTF_8);
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(
          new SecretKeySpec(
              properties.getWebhookSigningSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      return "t="
          + timestamp
          + ",v1="
          + HexFormat.of().formatHex(mac.doFinal(signed.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception ex) {
      throw new IllegalStateException(ex);
    }
  }
}
