package com.skoryk.projects.meetler.auth.mobile;

import com.skoryk.projects.meetler.auth.AuthService;
import com.skoryk.projects.meetler.auth.dto.AuthResponse;
import com.skoryk.projects.meetler.user.AppUser;
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

@Service
@RequiredArgsConstructor
public class MobileAuthExchangeService {

  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  private final MobileAuthExchangeCodeRepository repository;
  private final AuthService authService;

  @Value("${auth.mobile-exchange.ttl-seconds:120}")
  private long ttlSeconds;

  @Transactional
  public String createCode(AppUser user) {
    String rawCode = generateCode();
    repository.save(
        MobileAuthExchangeCode.builder()
            .user(user)
            .codeHash(hash(rawCode))
            .expiresAt(OffsetDateTime.now().plusSeconds(ttlSeconds))
            .build());
    return rawCode;
  }

  @Transactional
  public AuthResponse exchange(String rawCode) {
    MobileAuthExchangeCode code =
        repository
            .findForUpdateByCodeHash(hash(rawCode))
            .orElseThrow(() -> new IllegalArgumentException("Invalid mobile exchange code"));
    if (code.getUsedAt() != null || !code.getExpiresAt().isAfter(OffsetDateTime.now())) {
      throw new IllegalArgumentException("Mobile exchange code expired or already used");
    }

    code.setUsedAt(OffsetDateTime.now());
    repository.save(code);
    return authService.issueTokens(code.getUser());
  }

  private String generateCode() {
    byte[] bytes = new byte[32];
    SECURE_RANDOM.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  private String hash(String value) {
    try {
      byte[] digest =
          MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
      return java.util.HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is not available", exception);
    }
  }
}
