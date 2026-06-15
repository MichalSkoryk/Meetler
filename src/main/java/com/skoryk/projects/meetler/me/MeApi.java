package com.skoryk.projects.meetler.me;

import com.skoryk.projects.meetler.subscription.SubscriptionUsageResponse;
import com.skoryk.projects.meetler.user.AppUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Me")
@RequestMapping("/api/me")
public interface MeApi {

  @Operation(
      summary = "Get mobile bootstrap data",
      description =
          "Returns the authenticated user's profile, subscription usage, and connected external calendar status in one response.")
  @GetMapping("/bootstrap")
  ResponseEntity<MeBootstrapResponse> getBootstrap(@AuthenticationPrincipal AppUser user);

  @Operation(
      summary = "Get my subscription usage",
      description =
          "Returns the authenticated user's current subscription plan limits and current usage.")
  @GetMapping("/subscription/usage")
  ResponseEntity<SubscriptionUsageResponse> getSubscriptionUsage(
      @AuthenticationPrincipal AppUser user);
}
