package com.skoryk.projects.meetler.auth.password;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.skoryk.projects.meetler.auth.dto.PasswordResetConfirmRequest;
import com.skoryk.projects.meetler.auth.dto.PasswordResetRequest;
import com.skoryk.projects.meetler.auth.dto.PasswordResetResponse;
import com.skoryk.projects.meetler.auth.identity.UserAuthIdentityRepository;
import com.skoryk.projects.meetler.auth.token.RefreshTokenRepository;
import com.skoryk.projects.meetler.calendar.external.oauth.OAuthStateService;
import com.skoryk.projects.meetler.email.EmailService;
import com.skoryk.projects.meetler.user.AppUser;
import com.skoryk.projects.meetler.user.AppUserRepository;
import com.skoryk.projects.meetler.user.AppUserRole;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

  @Mock private AppUserRepository userRepository;
  @Mock private UserAuthIdentityRepository identityRepository;
  @Mock private PasswordResetTokenRepository tokenRepository;
  @Mock private RefreshTokenRepository refreshTokenRepository;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private EmailService emailService;
  @Mock private OAuthStateService returnUrlValidator;

  @InjectMocks private PasswordResetService service;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(service, "tokenTtlMinutes", 30L);
    ReflectionTestUtils.setField(
        service, "resetUrl", "http://localhost:8080/api/auth/password/reset/confirm");
  }

  @Test
  void requestResetDoesNotRevealUnknownEmail() {
    PasswordResetRequest request = request();
    when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());

    PasswordResetResponse response = service.requestReset(request);

    assertThat(response.getMessage()).isEqualTo("If the account exists, a reset link was created");
    assertThat(response.getResetLink()).isNull();
    verify(tokenRepository, never()).save(any());
    verify(emailService, never()).sendPasswordResetLink(anyString(), anyString(), any());
  }

  @Test
  void requestResetCreatesTokenAndSendsEmailForActiveUser() {
    PasswordResetRequest request = request();
    when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user()));
    when(tokenRepository.save(any(PasswordResetToken.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    PasswordResetResponse response = service.requestReset(request);

    assertThat(response.getResetLink())
        .startsWith("http://localhost:8080/api/auth/password/reset/confirm?token=");
    verify(emailService).sendPasswordResetLink(anyString(), anyString(), any(OffsetDateTime.class));

    ArgumentCaptor<PasswordResetToken> tokenCaptor =
        ArgumentCaptor.forClass(PasswordResetToken.class);
    verify(tokenRepository).save(tokenCaptor.capture());
    assertThat(tokenCaptor.getValue().getTokenHash()).hasSize(64);
    assertThat(tokenCaptor.getValue().getExpiresAt()).isAfter(OffsetDateTime.now());
  }

  @Test
  void requestResetUsesValidatedMobileReturnUrl() {
    PasswordResetRequest request = request();
    request.setReturnUrl("https://meetler.example/mobile/auth/reset");
    when(returnUrlValidator.validateReturnUrl(request.getReturnUrl()))
        .thenReturn(request.getReturnUrl());
    when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user()));
    when(tokenRepository.save(any(PasswordResetToken.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    PasswordResetResponse response = service.requestReset(request);

    assertThat(response.getResetLink())
        .startsWith("https://meetler.example/mobile/auth/reset?token=");
    verify(returnUrlValidator).validateReturnUrl(request.getReturnUrl());
  }

  @Test
  void confirmResetChangesPasswordRevokesSessionsAndConsumesToken() {
    AppUser user = user();
    user.setPasswordHash("old-hash");
    PasswordResetToken token =
        PasswordResetToken.builder()
            .user(user)
            .expiresAt(OffsetDateTime.now().plusMinutes(10))
            .build();
    PasswordResetConfirmRequest request = confirmRequest();

    when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));
    when(passwordEncoder.matches("new-password", "old-hash")).thenReturn(false);
    when(passwordEncoder.encode("new-password")).thenReturn("new-hash");
    when(identityRepository.findByUserAndProviderAndProviderUserId(any(), any(), anyString()))
        .thenReturn(Optional.empty());

    service.confirmReset(request);

    assertThat(user.getPasswordHash()).isEqualTo("new-hash");
    assertThat(token.getUsedAt()).isNotNull();
    verify(userRepository).save(user);
    verify(refreshTokenRepository).deleteByUser(user);
    verify(tokenRepository).save(token);
  }

  @Test
  void confirmResetRejectsUsedToken() {
    PasswordResetToken token =
        PasswordResetToken.builder()
            .user(user())
            .expiresAt(OffsetDateTime.now().plusMinutes(10))
            .usedAt(OffsetDateTime.now())
            .build();
    when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

    assertThatThrownBy(() -> service.confirmReset(confirmRequest()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Password reset token expired or already used");
  }

  private PasswordResetRequest request() {
    PasswordResetRequest request = new PasswordResetRequest();
    request.setEmail("TEST@example.com");
    return request;
  }

  private PasswordResetConfirmRequest confirmRequest() {
    PasswordResetConfirmRequest request = new PasswordResetConfirmRequest();
    request.setToken("raw-token");
    request.setNewPassword("new-password");
    return request;
  }

  private AppUser user() {
    return AppUser.builder()
        .id(UUID.randomUUID())
        .email("test@example.com")
        .role(AppUserRole.USER)
        .build();
  }
}
