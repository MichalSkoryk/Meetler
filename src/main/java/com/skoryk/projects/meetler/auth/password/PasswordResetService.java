package com.skoryk.projects.meetler.auth.password;

import com.skoryk.projects.meetler.auth.dto.PasswordResetConfirmRequest;
import com.skoryk.projects.meetler.auth.dto.PasswordResetRequest;
import com.skoryk.projects.meetler.auth.dto.PasswordResetResponse;
import com.skoryk.projects.meetler.auth.identity.UserAuthIdentity;
import com.skoryk.projects.meetler.auth.identity.UserAuthIdentityRepository;
import com.skoryk.projects.meetler.auth.token.RefreshTokenRepository;
import com.skoryk.projects.meetler.calendar.external.oauth.OAuthStateService;
import com.skoryk.projects.meetler.email.EmailService;
import com.skoryk.projects.meetler.user.AppUser;
import com.skoryk.projects.meetler.user.AppUserRepository;
import com.skoryk.projects.meetler.user.AuthProvider;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  private final AppUserRepository userRepository;
  private final UserAuthIdentityRepository identityRepository;
  private final PasswordResetTokenRepository tokenRepository;
  private final RefreshTokenRepository refreshTokenRepository;
  private final PasswordEncoder passwordEncoder;
  private final EmailService emailService;
  private final OAuthStateService returnUrlValidator;

  @Value("${auth.password-reset.token-ttl-minutes:30}")
  private long tokenTtlMinutes;

  @Value("${auth.password-reset.reset-url:http://localhost:8080/api/auth/password/reset/confirm}")
  private String resetUrl;

  @Transactional
  public PasswordResetResponse requestReset(PasswordResetRequest request) {
    String email = request.getEmail().toLowerCase();
    AppUser user = userRepository.findByEmail(email).orElse(null);
    if (user == null || user.getDeletedAt() != null) {
      return new PasswordResetResponse("If the account exists, a reset link was created", null);
    }

    String rawToken = generateToken();
    OffsetDateTime expiresAt = OffsetDateTime.now().plusMinutes(tokenTtlMinutes);
    tokenRepository.save(
        PasswordResetToken.builder()
            .user(user)
            .tokenHash(hash(rawToken))
            .expiresAt(expiresAt)
            .build());

    String resetLink = createResetLink(request.getReturnUrl(), rawToken);
    emailService.sendPasswordResetLink(email, resetLink, expiresAt);
    return new PasswordResetResponse("If the account exists, a reset link was created", resetLink);
  }

  @Transactional
  public void confirmReset(PasswordResetConfirmRequest request) {
    PasswordResetToken token =
        tokenRepository
            .findByTokenHash(hash(request.getToken()))
            .orElseThrow(() -> new IllegalArgumentException("Invalid password reset token"));

    if (token.getUsedAt() != null || token.getExpiresAt().isBefore(OffsetDateTime.now())) {
      throw new IllegalArgumentException("Password reset token expired or already used");
    }

    AppUser user = token.getUser();
    if (user.getDeletedAt() != null) {
      throw new IllegalArgumentException("User account is deleted");
    }
    if (user.getPasswordHash() != null
        && passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
      throw new IllegalArgumentException("New password must be different from current password");
    }

    user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
    userRepository.save(user);
    ensureInternalIdentity(user);
    refreshTokenRepository.deleteByUser(user);

    token.setUsedAt(OffsetDateTime.now());
    tokenRepository.save(token);
  }

  private void ensureInternalIdentity(AppUser user) {
    identityRepository
        .findByUserAndProviderAndProviderUserId(user, AuthProvider.INTERNAL, user.getEmail())
        .orElseGet(
            () ->
                identityRepository.save(
                    UserAuthIdentity.builder()
                        .user(user)
                        .provider(AuthProvider.INTERNAL)
                        .providerUserId(user.getEmail())
                        .providerEmail(user.getEmail())
                        .lastLoginAt(OffsetDateTime.now())
                        .build()));
  }

  private String createResetLink(String returnUrl, String rawToken) {
    if (returnUrl == null || returnUrl.isBlank()) {
      return resetUrl + "?token=" + URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
    }
    String validatedReturnUrl = returnUrlValidator.validateReturnUrl(returnUrl);
    return UriComponentsBuilder.fromUriString(validatedReturnUrl)
        .queryParam("token", rawToken)
        .build()
        .encode()
        .toUriString();
  }

  private String generateToken() {
    byte[] bytes = new byte[32];
    SECURE_RANDOM.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  private String hash(String rawToken) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
      StringBuilder value = new StringBuilder(hash.length * 2);
      for (byte b : hash) {
        value.append(String.format("%02x", b));
      }
      return value.toString();
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 is not available", ex);
    }
  }
}
