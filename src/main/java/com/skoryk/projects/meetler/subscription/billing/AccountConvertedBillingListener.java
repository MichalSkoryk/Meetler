package com.skoryk.projects.meetler.subscription.billing;

import com.skoryk.projects.meetler.user.AccountConvertedEvent;
import com.skoryk.projects.meetler.user.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class AccountConvertedBillingListener {

  private final BillingProperties properties;
  private final AppUserRepository userRepository;
  private final RevenueCatReconciliationService reconciliationService;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void synchronizeAfterConversion(AccountConvertedEvent event) {
    if (!properties.isConfigured()) {
      return;
    }
    userRepository
        .findById(event.userId())
        .ifPresent(
            user -> {
              try {
                reconciliationService.synchronize(user);
              } catch (RuntimeException ex) {
                log.warn(
                    "Could not synchronize RevenueCat after converting user {}",
                    event.userId(),
                    ex);
              }
            });
  }
}
