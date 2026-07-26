package com.skoryk.projects.meetler.subscription.billing;

import com.skoryk.projects.meetler.user.AppUser;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BillingEntitlementRepository extends JpaRepository<BillingEntitlement, UUID> {
  List<BillingEntitlement> findByCustomer(BillingCustomer customer);

  List<BillingEntitlement> findByCustomerUser(AppUser user);

  Optional<BillingEntitlement> findByCustomerAndEntitlementIdAndProductIdAndEnvironment(
      BillingCustomer customer,
      String entitlementId,
      String productId,
      BillingEnvironment environment);
}
