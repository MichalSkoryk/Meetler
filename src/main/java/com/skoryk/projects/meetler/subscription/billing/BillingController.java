package com.skoryk.projects.meetler.subscription.billing;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Hidden
@RestController
@RequestMapping("/api/billing")
@RequiredArgsConstructor
public class BillingController {

  private final RevenueCatWebhookService webhookService;

  @PostMapping("/revenuecat/webhook")
  public ResponseEntity<Void> revenueCatWebhook(
      @RequestBody byte[] rawBody,
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestHeader(value = "X-RevenueCat-Webhook-Signature", required = false) String signature) {
    webhookService.process(rawBody, authorization, signature);
    return ResponseEntity.ok().build();
  }
}
