package com.skoryk.projects.meetler.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.skoryk.projects.meetler.user.AuthProvider;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  @Mock private AppUserRepository userRepository;
  @Mock private UserAuthIdentityRepository identityRepository;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private JwtService jwtService;
  @Mock private RefreshTokenService refreshTokenService;

  @InjectMocks private AuthService service;

  @Test
  void registerRejectsDuplicateEmail() {
    RegisterRequest request = registerRequest();
    when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user()));

    assertThatThrownBy(() -> service.register(request))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Email already registered");

    verify(userRepository, never()).save(any());
  }

  @Test
  void registerCreatesUserIdentityAndTokens() {
    RegisterRequest request = registerRequest();
    when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
    when(passwordEncoder.encode("password123")).thenReturn("hash");
    when(userRepository.save(any(AppUser.class)))
        .thenAnswer(
            invocation -> {
              AppUser user = invocation.getArgument(0);
              user.setId(UUID.randomUUID());
              return user;
            });
    when(identityRepository.findByUserAndProviderAndProviderUserId(
            any(AppUser.class), any(), any()))
        .thenReturn(Optional.empty());
    when(jwtService.generateToken(any(), any())).thenReturn("access");
    when(refreshTokenService.createRefreshToken(any())).thenReturn("refresh");

    AuthResponse response = service.register(request);

    assertThat(response.getAccessToken()).isEqualTo("access");
    assertThat(response.getRefreshToken()).isEqualTo("refresh");
    verify(identityRepository).save(any(UserAuthIdentity.class));
  }

  @Test
  void loginRejectsDeletedUser() {
    LoginRequest request = loginRequest();
    AppUser user = user();
    user.setDeletedAt(OffsetDateTime.now());
    when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

    assertThatThrownBy(() -> service.login(request))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("User account is deleted");
  }

  @Test
  void loginRejectsWrongPassword() {
    LoginRequest request = loginRequest();
    AppUser user = user();
    user.setPasswordHash("hash");
    when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("password123", "hash")).thenReturn(false);

    assertThatThrownBy(() -> service.login(request))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Invalid credentials");
  }

  @Test
  void authenticateGoogleLinksExistingUserByEmail() {
    AppUser user = user();
    when(identityRepository.findByProviderAndProviderUserId(AuthProvider.GOOGLE, "google-id"))
        .thenReturn(Optional.empty());
    when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
    when(identityRepository.findByUserAndProviderAndProviderUserId(
            user, AuthProvider.GOOGLE, "google-id"))
        .thenReturn(Optional.empty());
    when(jwtService.generateToken(user.getId(), user.getEmail())).thenReturn("access");
    when(refreshTokenService.rotateRefreshToken(user)).thenReturn("refresh");

    AuthResponse response = service.authenticateGoogleUser("google-id", user.getEmail(), "Test");

    assertThat(response.getAccessToken()).isEqualTo("access");
    verify(identityRepository).save(any(UserAuthIdentity.class));
  }

  private RegisterRequest registerRequest() {
    RegisterRequest request = new RegisterRequest();
    request.setEmail("TEST@example.com");
    request.setName("Test");
    request.setPassword("password123");
    return request;
  }

  private LoginRequest loginRequest() {
    LoginRequest request = new LoginRequest();
    request.setEmail("TEST@example.com");
    request.setPassword("password123");
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
