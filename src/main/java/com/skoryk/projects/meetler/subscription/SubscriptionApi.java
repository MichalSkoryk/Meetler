package com.skoryk.projects.meetler.subscription;

import com.skoryk.projects.meetler.subscription.dto.AssignUserSubscriptionRequest;
import com.skoryk.projects.meetler.subscription.dto.UserSubscriptionResponse;
import com.skoryk.projects.meetler.user.AppUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Subscriptions")
@RequestMapping("/api/admin/users/{userId}/subscription")
public interface SubscriptionApi {

  @Operation(
      summary = "Get active user subscription",
      description = "Returns the active subscription for a user. Application admin only.")
  @GetMapping
  ResponseEntity<UserSubscriptionResponse> getActiveSubscription(
      @PathVariable UUID userId, @AuthenticationPrincipal AppUser requester);

  @Operation(
      summary = "Assign user subscription plan",
      description =
          "Upgrades or downgrades a user by cancelling the previous active subscription and assigning a new active plan. Application admin only.")
  @PatchMapping
  ResponseEntity<UserSubscriptionResponse> assignSubscriptionPlan(
      @PathVariable UUID userId,
      @Valid @RequestBody AssignUserSubscriptionRequest request,
      @AuthenticationPrincipal AppUser requester);
}
