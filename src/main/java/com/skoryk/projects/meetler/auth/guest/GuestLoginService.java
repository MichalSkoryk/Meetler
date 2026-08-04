package com.skoryk.projects.meetler.auth.guest;

import com.skoryk.projects.meetler.auth.dto.AuthResponse;
import com.skoryk.projects.meetler.auth.dto.GuestLoginRequest;
import com.skoryk.projects.meetler.auth.dto.GuestLoginResponse;
import com.skoryk.projects.meetler.auth.identity.UserAuthIdentity;
import com.skoryk.projects.meetler.auth.identity.UserAuthIdentityRepository;
import com.skoryk.projects.meetler.auth.jwt.JwtService;
import com.skoryk.projects.meetler.auth.token.RefreshTokenService;
import com.skoryk.projects.meetler.calendar.external.oauth.OAuthStateService;
import com.skoryk.projects.meetler.email.EmailService;
import com.skoryk.projects.meetler.user.AppUser;
import com.skoryk.projects.meetler.user.AppUserRepository;
import com.skoryk.projects.meetler.user.AppUserRole;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class GuestLoginService {

  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  private final AppUserRepository userRepository;
  private final UserAuthIdentityRepository identityRepository;
  private final GuestLoginTokenRepository tokenRepository;
  private final JwtService jwtService;
  private final RefreshTokenService refreshTokenService;
  private final EmailService emailService;
  private final OAuthStateService returnUrlValidator;

  @Value("${guest-login.token-ttl-minutes:30}")
  private long tokenTtlMinutes;

  @Value("${guest-login.login-url:http://localhost:8080/api/auth/guest/login}")
  private String loginUrl;

  @Transactional
  public GuestLoginResponse requestLoginLink(GuestLoginRequest request) {
    String email = request.getEmail().toLowerCase();
    AppUser user =
        userRepository
            .findByEmail(email)
            .map(this::validateExistingGuest)
            .orElseGet(() -> createGuestUser(email, request.getName()));

    String rawToken = generateToken();
    OffsetDateTime expiresAt = OffsetDateTime.now().plusMinutes(tokenTtlMinutes);
    tokenRepository.save(
        GuestLoginToken.builder()
            .user(user)
            .tokenHash(hash(rawToken))
            .expiresAt(expiresAt)
            .build());

    String link = createLoginLink(request.getReturnUrl(), rawToken);
    emailService.sendGuestLoginLink(email, link, expiresAt);

    return new GuestLoginResponse("Guest login link created", link);
  }

  @Transactional
  public AuthResponse loginWithToken(String rawToken) {
    GuestLoginToken token =
        tokenRepository
            .findByTokenHash(hash(rawToken))
            .orElseThrow(() -> new IllegalArgumentException("Invalid guest login token"));

    if (token.getUsedAt() != null || token.getExpiresAt().isBefore(OffsetDateTime.now())) {
      throw new IllegalArgumentException("Guest login token expired or already used");
    }

    AppUser user = token.getUser();
    if (user.getDeletedAt() != null || user.getRole() != AppUserRole.GUEST) {
      throw new IllegalArgumentException("Guest account is not active");
    }

    token.setUsedAt(OffsetDateTime.now());
    tokenRepository.save(token);

    String accessToken = jwtService.generateToken(user.getId(), user.getEmail());
    String refreshToken = refreshTokenService.rotateRefreshToken(user);
    return new AuthResponse(accessToken, refreshToken);
  }

  private AppUser validateExistingGuest(AppUser user) {
    if (user.getDeletedAt() != null) {
      throw new IllegalArgumentException("User account is deleted");
    }
    if (user.getRole() != AppUserRole.GUEST) {
      throw new IllegalArgumentException("Email is already registered");
    }
    return user;
  }

  private AppUser createGuestUser(String email, String name) {
    AppUser user =
        userRepository.save(
            AppUser.builder()
                .email(email)
                .name(resolveName(email, name))
                .role(AppUserRole.GUEST)
                .createdAt(OffsetDateTime.now())
                .build());

    identityRepository.save(
        UserAuthIdentity.builder()
            .user(user)
            .provider(AuthProvider.MAGIC_LINK)
            .providerUserId(email)
            .providerEmail(email)
            .lastLoginAt(OffsetDateTime.now())
            .build());

    return user;
  }

  private String resolveName(String email, String name) {
    if (name != null && !name.isBlank()) {
      return name;
    }
    return email.substring(0, email.indexOf('@'));
  }

  private String createLoginLink(String returnUrl, String rawToken) {
    if (returnUrl == null || returnUrl.isBlank()) {
      return loginUrl + "?token=" + URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
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
