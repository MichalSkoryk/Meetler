package com.skoryk.projects.meetler.auth;

import com.skoryk.projects.meetler.auth.dto.AuthResponse;
import com.skoryk.projects.meetler.auth.dto.LoginRequest;
import com.skoryk.projects.meetler.auth.dto.RegisterRequest;
import com.skoryk.projects.meetler.auth.identity.UserAuthIdentity;
import com.skoryk.projects.meetler.auth.identity.UserAuthIdentityRepository;
import com.skoryk.projects.meetler.auth.jwt.JwtService;
import com.skoryk.projects.meetler.auth.token.RefreshTokenService;
import com.skoryk.projects.meetler.user.AppUser;
import com.skoryk.projects.meetler.user.AppUserRepository;
import com.skoryk.projects.meetler.user.AppUserRole;
import com.skoryk.projects.meetler.user.AppUserService;
import com.skoryk.projects.meetler.user.AuthProvider;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

  private final AppUserRepository userRepository;
  private final UserAuthIdentityRepository identityRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final RefreshTokenService refreshTokenService;
  private final AppUserService appUserService;

  @Transactional
  public AuthResponse register(RegisterRequest request) {
    if (userRepository.findByEmail(request.getEmail().toLowerCase()).isPresent()) {
      throw new IllegalArgumentException("Email already registered");
    }

    AppUser user =
        AppUser.builder()
            .email(request.getEmail().toLowerCase())
            .name(request.getName())
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .role(AppUserRole.USER)
            .createdAt(OffsetDateTime.now())
            .build();

    userRepository.save(user);
    createIdentity(user, AuthProvider.INTERNAL, user.getEmail(), user.getEmail());

    String accessToken = jwtService.generateToken(user.getId(), user.getEmail());
    String refreshToken = refreshTokenService.createRefreshToken(user);

    return new AuthResponse(accessToken, refreshToken);
  }

  public AuthResponse login(LoginRequest request) {
    AppUser user =
        userRepository
            .findByEmail(request.getEmail().toLowerCase())
            .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

    ensureActive(user);

    if (user.getPasswordHash() == null
        || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
      throw new IllegalArgumentException("Invalid credentials");
    }

    String accessToken = jwtService.generateToken(user.getId(), user.getEmail());
    String refreshToken = refreshTokenService.rotateRefreshToken(user);

    return new AuthResponse(accessToken, refreshToken);
  }

  public AuthResponse refreshToken(String refreshToken) {
    AppUser user = refreshTokenService.validateAndRotate(refreshToken);

    String newAccessToken = jwtService.generateToken(user.getId(), user.getEmail());
    String newRefreshToken = refreshTokenService.rotateRefreshToken(user);

    return new AuthResponse(newAccessToken, newRefreshToken);
  }

  public void logout(String refreshToken) {
    refreshTokenService.revoke(refreshToken);
  }

  @Transactional
  public AuthResponse authenticateGoogleUser(String providerUserId, String email, String name) {
    return authenticateExternalUser(providerUserId, email, name, AuthProvider.GOOGLE);
  }

  @Transactional
  public AuthResponse authenticateMicrosoftUser(String providerUserId, String email, String name) {
    return authenticateExternalUser(providerUserId, email, name, AuthProvider.MICROSOFT);
  }

  private AuthResponse authenticateExternalUser(
      String providerUserId, String email, String name, AuthProvider authProvider) {
    if (providerUserId == null || providerUserId.isBlank()) {
      throw new IllegalArgumentException(authProvider + " user id was not returned");
    }

    String normalizedEmail = email.toLowerCase();
    OffsetDateTime now = OffsetDateTime.now();

    AppUser userFromIdentity =
        identityRepository
            .findByProviderAndProviderUserId(authProvider, providerUserId)
            .map(
                identity -> {
                  identity.setProviderEmail(normalizedEmail);
                  identity.setLastLoginAt(now);
                  identityRepository.save(identity);
                  return identity.getUser();
                })
            .orElse(null);

    if (userFromIdentity != null) {
      ensureActive(userFromIdentity);
      return issueTokens(appUserService.convertGuestToUser(userFromIdentity));
    }

    AppUser user =
        userRepository
            .findByEmail(normalizedEmail)
            .map(
                existingUser -> {
                  ensureActive(existingUser);
                  return appUserService.convertGuestToUser(existingUser);
                })
            .orElseGet(
                () ->
                    userRepository.save(
                        AppUser.builder()
                            .email(normalizedEmail)
                            .name(name)
                            .role(AppUserRole.USER)
                            .createdAt(OffsetDateTime.now())
                            .build()));

    createIdentity(user, authProvider, providerUserId, normalizedEmail);
    return issueTokens(user);
  }

  private UserAuthIdentity createIdentity(
      AppUser user, AuthProvider provider, String providerUserId, String providerEmail) {
    return identityRepository
        .findByUserAndProviderAndProviderUserId(user, provider, providerUserId)
        .orElseGet(
            () ->
                identityRepository.save(
                    UserAuthIdentity.builder()
                        .user(user)
                        .provider(provider)
                        .providerUserId(providerUserId)
                        .providerEmail(providerEmail)
                        .lastLoginAt(OffsetDateTime.now())
                        .build()));
  }

  private void ensureActive(AppUser user) {
    if (user.getDeletedAt() != null) {
      throw new IllegalArgumentException("User account is deleted");
    }
  }

  private AuthResponse issueTokens(AppUser user) {
    String accessToken = jwtService.generateToken(user.getId(), user.getEmail());
    String refreshToken = refreshTokenService.rotateRefreshToken(user);

    return new AuthResponse(accessToken, refreshToken);
  }
}
