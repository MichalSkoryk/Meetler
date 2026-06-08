package com.skoryk.projects.meetler.auth.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.skoryk.projects.meetler.user.AppUser;
import com.skoryk.projects.meetler.user.AppUserRole;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

  @Mock private RefreshTokenRepository repo;

  @InjectMocks private RefreshTokenService service;

  @Test
  void rotateDeletesOldTokensAndCreatesNewOne() {
    AppUser user = user();
    when(repo.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

    String token = service.rotateRefreshToken(user);

    assertThat(token).isNotBlank();
    verify(repo).deleteByUser(user);
    verify(repo).save(any(RefreshToken.class));
  }

  @Test
  void validateRejectsExpiredToken() {
    RefreshToken token =
        RefreshToken.builder()
            .token("token")
            .user(user())
            .expiresAt(OffsetDateTime.now().minusMinutes(1))
            .revoked(false)
            .build();
    when(repo.findByToken("token")).thenReturn(Optional.of(token));

    assertThatThrownBy(() -> service.validateAndRotate("token"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Refresh token expired or revoked");
  }

  @Test
  void revokeMarksTokenRevoked() {
    RefreshToken token =
        RefreshToken.builder()
            .token("token")
            .user(user())
            .expiresAt(OffsetDateTime.now().plusDays(1))
            .revoked(false)
            .build();
    when(repo.findByToken("token")).thenReturn(Optional.of(token));

    service.revoke("token");

    assertThat(token.isRevoked()).isTrue();
    verify(repo).save(token);
  }

  private AppUser user() {
    return AppUser.builder()
        .id(UUID.randomUUID())
        .email("test@example.com")
        .role(AppUserRole.USER)
        .build();
  }
}
