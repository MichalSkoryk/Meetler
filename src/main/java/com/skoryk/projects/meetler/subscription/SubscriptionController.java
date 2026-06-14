package com.skoryk.projects.meetler.subscription;

import com.skoryk.projects.meetler.subscription.dto.AssignUserSubscriptionRequest;
import com.skoryk.projects.meetler.subscription.dto.UserSubscriptionResponse;
import com.skoryk.projects.meetler.user.AppUser;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SubscriptionController implements SubscriptionApi {

  private final UserSubscriptionService userSubscriptionService;

  @Override
  public ResponseEntity<UserSubscriptionResponse> getActiveSubscription(
      UUID userId, AppUser requester) {
    return ResponseEntity.ok(userSubscriptionService.getActiveSubscription(userId, requester));
  }

  @Override
  public ResponseEntity<UserSubscriptionResponse> assignSubscriptionPlan(
      UUID userId, AssignUserSubscriptionRequest request, AppUser requester) {
    return ResponseEntity.ok(
        userSubscriptionService.assignPlan(userId, request.getPlanCode(), requester));
  }
}
