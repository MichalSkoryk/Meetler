package com.skoryk.projects.meetler.auth.token;

import com.skoryk.projects.meetler.user.AppUser;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

  private final RefreshTokenRepository repo;

  public String createRefreshToken(AppUser user) {
    RefreshToken token =
        RefreshToken.builder()
            .token(UUID.randomUUID().toString())
            .user(user)
            .expiresAt(OffsetDateTime.now().plusDays(30))
            .revoked(false)
            .build();

    repo.save(token);
    return token.getToken();
  }

  @Transactional
  public String rotateRefreshToken(AppUser user) {
    // Remove all old tokens for this user
    repo.deleteByUser(user);

    // Create a new one
    return createRefreshToken(user);
  }

  public AppUser validateAndRotate(String tokenValue) {
    RefreshToken token =
        repo.findByToken(tokenValue)
            .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

    if (token.isRevoked() || token.getExpiresAt().isBefore(OffsetDateTime.now())) {
      throw new IllegalArgumentException("Refresh token expired or revoked");
    }

    return token.getUser();
  }

  @Transactional
  public void revoke(String tokenValue) {
    RefreshToken token =
        repo.findByToken(tokenValue)
            .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

    token.setRevoked(true);
    repo.save(token);
  }
}
