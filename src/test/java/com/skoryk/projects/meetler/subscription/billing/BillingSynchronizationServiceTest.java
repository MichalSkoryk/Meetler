package com.skoryk.projects.meetler.subscription.billing;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.skoryk.projects.meetler.user.AppUser;
import com.skoryk.projects.meetler.user.AppUserRole;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BillingSynchronizationServiceTest {

  @Mock private RevenueCatReconciliationService reconciliationService;
  @InjectMocks private BillingSynchronizationService service;

  @Test
  void rejectsGuestSynchronization() {
    AppUser guest = user(AppUserRole.GUEST);

    assertThatThrownBy(() -> service.synchronizeCurrentUser(guest))
        .isInstanceOf(GuestBillingNotAllowedException.class);
    verifyNoInteractions(reconciliationService);
  }

  @Test
  void synchronizesRegularUser() {
    AppUser user = user(AppUserRole.USER);

    service.synchronizeCurrentUser(user);

    verify(reconciliationService).synchronize(user);
  }

  private AppUser user(AppUserRole role) {
    return AppUser.builder().id(UUID.randomUUID()).email("billing@example.com").role(role).build();
  }
}
