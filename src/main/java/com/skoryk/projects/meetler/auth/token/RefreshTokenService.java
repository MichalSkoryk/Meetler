package com.skoryk.projects.meetler.auth.token;

import com.skoryk.projects.meetler.user.AppUser;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

  private final RefreshTokenRepository repo;

  @Value("${auth.refresh-token.ttl-days:30}")
  private long refreshTokenTtlDays;

  public String createRefreshToken(AppUser user) {
    RefreshToken token =
        RefreshToken.builder()
            .token(UUID.randomUUID().toString())
            .user(user)
            .expiresAt(OffsetDateTime.now().plusDays(refreshTokenTtlDays))
            .revoked(false)
            .build();

    repo.save(token);
    return token.getToken();
  }

  @Transactional
  public RefreshTokenRotation rotateRefreshToken(String tokenValue) {
    RefreshToken token =
        repo.findByTokenForUpdate(tokenValue)
            .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

    if (token.isRevoked() || token.getExpiresAt().isBefore(OffsetDateTime.now())) {
      throw new IllegalArgumentException("Refresh token expired or revoked");
    }

    String rotatedToken = UUID.randomUUID().toString();
    token.setToken(rotatedToken);
    token.setExpiresAt(OffsetDateTime.now().plusDays(refreshTokenTtlDays));
    repo.save(token);
    return new RefreshTokenRotation(token.getUser(), rotatedToken);
  }

  @Transactional
  public void revoke(String tokenValue) {
    RefreshToken token =
        repo.findByToken(tokenValue)
            .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

    token.setRevoked(true);
    repo.save(token);
  }

  public record RefreshTokenRotation(AppUser user, String token) {}
}
