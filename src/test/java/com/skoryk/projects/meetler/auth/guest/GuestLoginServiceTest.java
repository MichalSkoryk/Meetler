package com.skoryk.projects.meetler.auth.guest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class GuestLoginServiceTest {

  @Mock private AppUserRepository userRepository;
  @Mock private UserAuthIdentityRepository identityRepository;
  @Mock private GuestLoginTokenRepository tokenRepository;
  @Mock private JwtService jwtService;
  @Mock private RefreshTokenService refreshTokenService;
  @Mock private EmailService emailService;
  @Mock private OAuthStateService returnUrlValidator;

  @InjectMocks private GuestLoginService service;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(service, "tokenTtlMinutes", 15L);
    ReflectionTestUtils.setField(service, "loginUrl", "http://localhost:8080/api/auth/guest/login");
  }

  @Test
  void requestLoginLinkCreatesGuestUserAndOneTimeToken() {
    GuestLoginRequest request = request();
    when(userRepository.findByEmail("guest@example.com")).thenReturn(Optional.empty());
    when(userRepository.save(any(AppUser.class)))
        .thenAnswer(
            invocation -> {
              AppUser user = invocation.getArgument(0);
              user.setId(UUID.randomUUID());
              return user;
            });
    when(tokenRepository.save(any(GuestLoginToken.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    GuestLoginResponse response = service.requestLoginLink(request);

    assertThat(response.getLoginLink())
        .startsWith("http://localhost:8080/api/auth/guest/login?token=");
    verify(emailService).sendGuestLoginLink(anyString(), anyString(), any(OffsetDateTime.class));
    verify(identityRepository).save(any(UserAuthIdentity.class));

    ArgumentCaptor<AppUser> userCaptor = ArgumentCaptor.forClass(AppUser.class);
    verify(userRepository).save(userCaptor.capture());
    assertThat(userCaptor.getValue().getRole()).isEqualTo(AppUserRole.GUEST);
    assertThat(userCaptor.getValue().getPasswordHash()).isNull();

    ArgumentCaptor<GuestLoginToken> tokenCaptor = ArgumentCaptor.forClass(GuestLoginToken.class);
    verify(tokenRepository).save(tokenCaptor.capture());
    assertThat(tokenCaptor.getValue().getTokenHash()).hasSize(64);
    assertThat(tokenCaptor.getValue().getExpiresAt()).isAfter(OffsetDateTime.now());
  }

  @Test
  void requestLoginLinkRejectsExistingRegisteredUser() {
    GuestLoginRequest request = request();
    when(userRepository.findByEmail("guest@example.com"))
        .thenReturn(Optional.of(user(AppUserRole.USER)));

    assertThatThrownBy(() -> service.requestLoginLink(request))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Email is already registered");

    verify(tokenRepository, never()).save(any());
  }

  @Test
  void requestLoginLinkUsesValidatedMobileReturnUrl() {
    GuestLoginRequest request = request();
    request.setReturnUrl("https://meetler.example/mobile/auth/guest");
    when(returnUrlValidator.validateReturnUrl(request.getReturnUrl()))
        .thenReturn(request.getReturnUrl());
    when(userRepository.findByEmail("guest@example.com")).thenReturn(Optional.empty());
    when(userRepository.save(any(AppUser.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(tokenRepository.save(any(GuestLoginToken.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    GuestLoginResponse response = service.requestLoginLink(request);

    assertThat(response.getLoginLink())
        .startsWith("https://meetler.example/mobile/auth/guest?token=");
    verify(returnUrlValidator).validateReturnUrl(request.getReturnUrl());
  }

  @Test
  void loginWithTokenConsumesTokenAndIssuesAuthTokens() {
    AppUser user = user(AppUserRole.GUEST);
    GuestLoginToken token =
        GuestLoginToken.builder()
            .user(user)
            .expiresAt(OffsetDateTime.now().plusMinutes(10))
            .build();
    when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));
    when(jwtService.generateToken(user.getId(), user.getEmail())).thenReturn("access");
    when(refreshTokenService.rotateRefreshToken(user)).thenReturn("refresh");

    AuthResponse response = service.loginWithToken("raw-token");

    assertThat(response.getAccessToken()).isEqualTo("access");
    assertThat(response.getRefreshToken()).isEqualTo("refresh");
    assertThat(token.getUsedAt()).isNotNull();
    verify(tokenRepository).save(token);
  }

  private GuestLoginRequest request() {
    GuestLoginRequest request = new GuestLoginRequest();
    request.setEmail("GUEST@example.com");
    request.setName("Guest User");
    return request;
  }

  private AppUser user(AppUserRole role) {
    return AppUser.builder()
        .id(UUID.randomUUID())
        .email("guest@example.com")
        .name("Guest User")
        .role(role)
        .build();
  }
}
