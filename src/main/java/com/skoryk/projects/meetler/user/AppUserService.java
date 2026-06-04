package com.skoryk.projects.meetler.user;

import com.skoryk.projects.meetler.auth.identity.UserAuthIdentity;
import com.skoryk.projects.meetler.auth.identity.UserAuthIdentityRepository;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AppUserService {

  private final AppUserRepository appUserRepository;
  private final UserAuthIdentityRepository identityRepository;
  private final PasswordEncoder passwordEncoder;

  public AppUser createUser(String email) {
    AppUser appUser =
        AppUser.builder()
            .email(email.toLowerCase())
            .role(AppUserRole.GUEST)
            .createdAt(OffsetDateTime.now())
            .build();

    return appUserRepository.save(appUser);
  }

  public Optional<AppUser> findByEmail(String email) {
    return appUserRepository.findByEmail(email.toLowerCase());
  }

  public Optional<AppUser> findById(UUID id) {
    return appUserRepository.findById(id);
  }

  public AppUser upgradeToUser(UUID userId) {
    AppUser appUser =
        appUserRepository
            .findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

    if (appUser.getRole() == AppUserRole.USER) {
      return appUser;
    }

    appUser.setRole(AppUserRole.USER);
    appUser.setUpgradedAt(OffsetDateTime.now());

    return appUserRepository.save(appUser);
  }

  public AppUser updateName(UUID userId, String name) {
    AppUser appUser =
        appUserRepository
            .findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

    appUser.setName(name);
    return appUserRepository.save(appUser);
  }

  public void softDelete(UUID userId) {
    AppUser appUser =
        appUserRepository
            .findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

    appUser.setDeletedAt(OffsetDateTime.now());
    appUserRepository.save(appUser);
  }

  public void changePassword(AppUser user, String currentPassword, String newPassword) {
    AppUser appUser =
        appUserRepository
            .findById(user.getId())
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

    if (appUser.getDeletedAt() != null) {
      throw new IllegalArgumentException("User account is deleted");
    }

    if (appUser.getPasswordHash() == null
        || !passwordEncoder.matches(currentPassword, appUser.getPasswordHash())) {
      throw new IllegalArgumentException("Invalid current password");
    }

    if (passwordEncoder.matches(newPassword, appUser.getPasswordHash())) {
      throw new IllegalArgumentException("New password must be different from current password");
    }

    appUser.setPasswordHash(passwordEncoder.encode(newPassword));
    appUserRepository.save(appUser);
    ensureInternalIdentity(appUser);
  }

  private void ensureInternalIdentity(AppUser appUser) {
    identityRepository
        .findByUserAndProviderAndProviderUserId(appUser, AuthProvider.INTERNAL, appUser.getEmail())
        .orElseGet(
            () ->
                identityRepository.save(
                    UserAuthIdentity.builder()
                        .user(appUser)
                        .provider(AuthProvider.INTERNAL)
                        .providerUserId(appUser.getEmail())
                        .providerEmail(appUser.getEmail())
                        .lastLoginAt(OffsetDateTime.now())
                        .build()));
  }

  public void setInitialPassword(AppUser user, String initialPassword) {
    AppUser appUser =
        appUserRepository
            .findById(user.getId())
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

    if (appUser.getDeletedAt() != null)
      throw new IllegalArgumentException("User account is deleted");

    if (appUser.getPasswordHash() != null) {
      throw new IllegalArgumentException("This user already has password");
    }

    appUser.setPasswordHash(passwordEncoder.encode(initialPassword));
    appUserRepository.save(appUser);
    ensureInternalIdentity(appUser);
  }
}
