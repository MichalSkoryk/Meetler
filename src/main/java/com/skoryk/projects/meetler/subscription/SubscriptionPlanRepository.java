package com.skoryk.projects.meetler.subscription;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, UUID> {

  Optional<SubscriptionPlan> findByCodeAndActiveTrue(String code);
}
