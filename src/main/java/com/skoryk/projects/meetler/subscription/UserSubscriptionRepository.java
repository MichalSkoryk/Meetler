package com.skoryk.projects.meetler.subscription;

import com.skoryk.projects.meetler.user.AppUser;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSubscriptionRepository extends JpaRepository<UserSubscription, UUID> {

  Optional<UserSubscription> findFirstByUserAndStatusOrderByStartedAtDesc(
      AppUser user, SubscriptionStatus status);
}
