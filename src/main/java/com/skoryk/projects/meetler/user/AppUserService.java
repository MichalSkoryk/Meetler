package com.skoryk.projects.meetler.user;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AppUserService {

  private final AppUserRepository appUserRepository;

  public AppUser createUser(String email, AuthProvider provider) {
    AppUser appUser =
        AppUser.builder()
            .email(email.toLowerCase())
            .role(AppUserRole.GUEST)
            .authProvider(provider)
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
}
