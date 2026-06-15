package com.skoryk.projects.meetler.auth;

import com.skoryk.projects.meetler.auth.dto.AuthResponse;
import com.skoryk.projects.meetler.auth.dto.GuestLoginRequest;
import com.skoryk.projects.meetler.auth.dto.GuestLoginResponse;
import com.skoryk.projects.meetler.auth.dto.LoginRequest;
import com.skoryk.projects.meetler.auth.dto.PasswordResetConfirmRequest;
import com.skoryk.projects.meetler.auth.dto.PasswordResetRequest;
import com.skoryk.projects.meetler.auth.dto.PasswordResetResponse;
import com.skoryk.projects.meetler.auth.dto.RefreshTokenRequest;
import com.skoryk.projects.meetler.auth.dto.RegisterRequest;
import com.skoryk.projects.meetler.auth.google.GoogleAuthService;
import com.skoryk.projects.meetler.auth.guest.GuestLoginService;
import com.skoryk.projects.meetler.auth.microsoft.MicrosoftAuthService;
import com.skoryk.projects.meetler.auth.password.PasswordResetService;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequiredArgsConstructor
public class AuthController implements AuthApi {

  private final AuthService authService;
  private final GuestLoginService guestLoginService;
  private final PasswordResetService passwordResetService;
  private final GoogleAuthService googleAuthService;
  private final MicrosoftAuthService microsoftAuthService;

  @Value("${auth.password-reset.frontend-url:http://localhost:8081}")
  private String passwordResetFrontendUrl;

  @Override
  public ResponseEntity<AuthResponse> register(RegisterRequest request) {
    AuthResponse response = authService.register(request);
    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<AuthResponse> login(LoginRequest request) {
    AuthResponse response = authService.login(request);
    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<AuthResponse> refresh(RefreshTokenRequest request) {
    AuthResponse response = authService.refreshToken(request.getRefreshToken());
    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<Void> logout(RefreshTokenRequest request) {
    authService.logout(request.getRefreshToken());
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<GuestLoginResponse> requestGuestLogin(GuestLoginRequest request) {
    return ResponseEntity.ok(guestLoginService.requestLoginLink(request));
  }

  @Override
  public ResponseEntity<AuthResponse> guestLogin(String token) {
    return ResponseEntity.ok(guestLoginService.loginWithToken(token));
  }

  @Override
  public ResponseEntity<PasswordResetResponse> requestPasswordReset(PasswordResetRequest request) {
    return ResponseEntity.ok(passwordResetService.requestReset(request));
  }

  @Override
  public ResponseEntity<Void> confirmPasswordReset(PasswordResetConfirmRequest request) {
    passwordResetService.confirmReset(request);
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<Void> openPasswordResetPage(String token) {
    URI redirectUri =
        UriComponentsBuilder.fromUriString(passwordResetFrontendUrl)
            .queryParam("resetPassword", "true")
            .queryParam("token", token)
            .build()
            .toUri();
    return ResponseEntity.status(302).location(redirectUri).build();
  }

  @Override
  public ResponseEntity<Void> googleLogin(String returnUrl) {
    URI authorizationUri = googleAuthService.buildAuthorizationUri(returnUrl);
    return ResponseEntity.status(302).location(authorizationUri).build();
  }

  @Override
  public ResponseEntity<?> googleCallback(
      String code, String state, String error, String errorDescription) {
    if (error != null) {
      throw new IllegalArgumentException(
          "Google OAuth failed: " + oauthError(error, errorDescription));
    }
    if (code == null || code.isBlank()) {
      throw new IllegalArgumentException(
          "Google OAuth callback did not include an authorization code");
    }
    return oauthLoginResponse(googleAuthService.handleCallbackWithRedirect(code, state));
  }

  @Override
  public ResponseEntity<Void> microsoftLogin(String returnUrl) {
    URI authorizationUri = microsoftAuthService.buildAuthorizationUri(returnUrl);
    return ResponseEntity.status(302).location(authorizationUri).build();
  }

  @Override
  public ResponseEntity<?> microsoftCallback(
      String code, String state, String error, String errorDescription) {
    if (error != null) {
      throw new IllegalArgumentException(
          "Microsoft OAuth failed: " + oauthError(error, errorDescription));
    }
    if (code == null || code.isBlank()) {
      throw new IllegalArgumentException(
          "Microsoft OAuth callback did not include an authorization code");
    }
    return oauthLoginResponse(microsoftAuthService.handleCallbackWithRedirect(code, state));
  }

  private ResponseEntity<?> oauthLoginResponse(OAuthLoginResult result) {
    if (result.returnUrl() == null) {
      return ResponseEntity.ok(result.authResponse());
    }

    URI redirectUri =
        org.springframework.web.util.UriComponentsBuilder.fromUriString(result.returnUrl())
            .queryParam("accessToken", result.authResponse().getAccessToken())
            .queryParam("refreshToken", result.authResponse().getRefreshToken())
            .build()
            .toUri();
    return ResponseEntity.status(302).location(redirectUri).build();
  }

  private String oauthError(String error, String errorDescription) {
    if (errorDescription == null || errorDescription.isBlank()) {
      return error;
    }
    return error + " - " + errorDescription;
  }
}
