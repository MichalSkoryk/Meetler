package com.skoryk.projects.meetler.subscription.billing;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RevenueCatWebhookVerifier {

  private final BillingProperties properties;
  private final Clock clock;

  @Autowired
  public RevenueCatWebhookVerifier(BillingProperties properties) {
    this(properties, Clock.systemUTC());
  }

  RevenueCatWebhookVerifier(BillingProperties properties, Clock clock) {
    this.properties = properties;
    this.clock = clock;
  }

  public void verify(byte[] rawBody, String authorization, String signatureHeader) {
    if (!properties.isConfigured()) {
      throw new BillingUnavailableException("Billing is not configured");
    }
    if (!secureEquals(properties.getWebhookAuthorization(), authorization)) {
      throw new InvalidBillingWebhookException("Invalid webhook authorization");
    }

    SignatureParts parts = parseSignature(signatureHeader);
    long now = clock.instant().getEpochSecond();
    long tolerance = properties.getWebhookToleranceSeconds();
    if (parts.timestamp() < now - tolerance || parts.timestamp() > now + tolerance) {
      throw new InvalidBillingWebhookException("Webhook signature timestamp is outside tolerance");
    }

    String signedPayload = parts.timestamp() + "." + new String(rawBody, StandardCharsets.UTF_8);
    String expected = hmacHex(properties.getWebhookSigningSecret(), signedPayload);
    boolean valid =
        parts.signatures().stream().anyMatch(signature -> secureEquals(expected, signature));
    if (!valid) {
      throw new InvalidBillingWebhookException("Invalid webhook signature");
    }
  }

  private SignatureParts parseSignature(String header) {
    if (header == null || header.isBlank()) {
      throw new InvalidBillingWebhookException("Missing webhook signature");
    }

    Long timestamp = null;
    List<String> signatures = new ArrayList<>();
    for (String part : header.split(",")) {
      String[] pair = part.trim().split("=", 2);
      if (pair.length != 2) {
        continue;
      }
      if (pair[0].equals("t")) {
        try {
          timestamp = Long.parseLong(pair[1]);
        } catch (NumberFormatException ex) {
          throw new InvalidBillingWebhookException("Invalid webhook signature timestamp");
        }
      } else if (pair[0].equals("v1")) {
        signatures.add(pair[1]);
      }
    }
    if (timestamp == null || signatures.isEmpty()) {
      throw new InvalidBillingWebhookException("Invalid webhook signature format");
    }
    return new SignatureParts(timestamp, List.copyOf(signatures));
  }

  private String hmacHex(String secret, String value) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
    } catch (GeneralSecurityException ex) {
      throw new IllegalStateException("Could not verify webhook signature", ex);
    }
  }

  private boolean secureEquals(String expected, String actual) {
    if (expected == null || actual == null) {
      return false;
    }
    return MessageDigest.isEqual(
        expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8));
  }

  private record SignatureParts(long timestamp, List<String> signatures) {}
}
