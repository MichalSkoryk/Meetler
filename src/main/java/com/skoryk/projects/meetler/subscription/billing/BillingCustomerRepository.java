package com.skoryk.projects.meetler.subscription.billing;

import com.skoryk.projects.meetler.user.AppUser;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BillingCustomerRepository extends JpaRepository<BillingCustomer, UUID> {
  Optional<BillingCustomer> findByUser(AppUser user);
}
