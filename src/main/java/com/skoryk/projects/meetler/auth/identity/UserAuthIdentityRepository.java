package com.skoryk.projects.meetler.auth.identity;

import com.skoryk.projects.meetler.user.AppUser;
import com.skoryk.projects.meetler.user.AuthProvider;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAuthIdentityRepository extends JpaRepository<UserAuthIdentity, UUID> {

  Optional<UserAuthIdentity> findByProviderAndProviderUserId(
      AuthProvider provider, String providerUserId);

  Optional<UserAuthIdentity> findByUserAndProviderAndProviderUserId(
      AppUser user, AuthProvider provider, String providerUserId);

  List<UserAuthIdentity> findByUser(AppUser user);
}
