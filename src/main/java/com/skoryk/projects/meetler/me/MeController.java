package com.skoryk.projects.meetler.me;

import com.skoryk.projects.meetler.subscription.SubscriptionUsageResponse;
import com.skoryk.projects.meetler.user.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MeController implements MeApi {

  private final MeService meService;

  @Override
  public ResponseEntity<MeBootstrapResponse> getBootstrap(AppUser user) {
    return ResponseEntity.ok(meService.getBootstrap(user));
  }

  @Override
  public ResponseEntity<SubscriptionUsageResponse> getSubscriptionUsage(AppUser user) {
    return ResponseEntity.ok(meService.getSubscriptionUsage(user));
  }

  @Override
  public ResponseEntity<SubscriptionUsageResponse> synchronizeSubscription(AppUser user) {
    return ResponseEntity.ok(meService.synchronizeSubscription(user));
  }
}
