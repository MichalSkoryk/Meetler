package com.skoryk.projects.meetler.subscription.billing;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "billing.revenuecat")
public class BillingProperties {

  private BillingMode mode = BillingMode.DISABLED;
  private String apiUrl = "https://api.revenuecat.com/v1";
  private String secretApiKey = "";
  private String webhookAuthorization = "";
  private String webhookSigningSecret = "";
  private long webhookToleranceSeconds = 300;
  private String plusEntitlementId = "plus";
  private String proEntitlementId = "pro";

  public boolean isConfigured() {
    return mode != BillingMode.DISABLED
        && hasText(secretApiKey)
        && hasText(webhookAuthorization)
        && hasText(webhookSigningSecret);
  }

  public String planCodeFor(String entitlementId) {
    if (entitlementId == null) {
      return null;
    }
    if (entitlementId.equalsIgnoreCase(proEntitlementId)) {
      return "PRO";
    }
    if (entitlementId.equalsIgnoreCase(plusEntitlementId)) {
      return "PLUS";
    }
    return null;
  }

  private boolean hasText(String value) {
    return value != null && !value.isBlank();
  }
}
