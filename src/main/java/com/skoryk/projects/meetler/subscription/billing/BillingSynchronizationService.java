package com.skoryk.projects.meetler.subscription.billing;

import com.skoryk.projects.meetler.user.AppUser;
import com.skoryk.projects.meetler.user.AppUserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BillingSynchronizationService {

  private final RevenueCatReconciliationService reconciliationService;

  public void synchronizeCurrentUser(AppUser user) {
    if (user.getRole() == AppUserRole.GUEST) {
      throw new GuestBillingNotAllowedException();
    }
    reconciliationService.synchronize(user);
  }
}
