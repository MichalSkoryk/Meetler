package com.skoryk.projects.meetler.auth;

import com.skoryk.projects.meetler.auth.dto.AuthResponse;
import com.skoryk.projects.meetler.auth.dto.LoginRequest;
import com.skoryk.projects.meetler.auth.dto.RefreshTokenRequest;
import com.skoryk.projects.meetler.auth.dto.RegisterRequest;
import com.skoryk.projects.meetler.auth.google.GoogleAuthService;
import com.skoryk.projects.meetler.auth.microsoft.MicrosoftAuthService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Authentication")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;
  private final GoogleAuthService googleAuthService;
  private final MicrosoftAuthService microsoftAuthService;

  @PostMapping("/register")
  public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
    AuthResponse response = authService.register(request);
    return ResponseEntity.ok(response);
  }

  @PostMapping("/login")
  public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
    AuthResponse response = authService.login(request);
    return ResponseEntity.ok(response);
  }

  @PostMapping("/refresh")
  public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
    AuthResponse response = authService.refreshToken(request.getRefreshToken());
    return ResponseEntity.ok(response);
  }

  @PostMapping("/logout")
  public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
    authService.logout(request.getRefreshToken());
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/google/login")
  public ResponseEntity<Void> googleLogin() {
    URI authorizationUri = googleAuthService.buildAuthorizationUri();
    return ResponseEntity.status(302).location(authorizationUri).build();
  }

  @GetMapping("/google/callback")
  public ResponseEntity<AuthResponse> googleCallback(
      @RequestParam(required = false) String code,
      @RequestParam String state,
      @RequestParam(required = false) String error,
      @RequestParam(name = "error_description", required = false) String errorDescription) {
    if (error != null) {
      throw new IllegalArgumentException(
          "Google OAuth failed: " + oauthError(error, errorDescription));
    }
    if (code == null || code.isBlank()) {
      throw new IllegalArgumentException(
          "Google OAuth callback did not include an authorization code");
    }
    return ResponseEntity.ok(googleAuthService.handleCallback(code, state));
  }

  @GetMapping("/microsoft/login")
  public ResponseEntity<Void> microsoftLogin() {
    URI authorizationUri = microsoftAuthService.buildAuthorizationUri();
    return ResponseEntity.status(302).location(authorizationUri).build();
  }

  @GetMapping("/microsoft/callback")
  public ResponseEntity<AuthResponse> microsoftCallback(
      @RequestParam(required = false) String code,
      @RequestParam String state,
      @RequestParam(required = false) String error,
      @RequestParam(name = "error_description", required = false) String errorDescription) {
    if (error != null) {
      throw new IllegalArgumentException(
          "Microsoft OAuth failed: " + oauthError(error, errorDescription));
    }
    if (code == null || code.isBlank()) {
      throw new IllegalArgumentException(
          "Microsoft OAuth callback did not include an authorization code");
    }
    return ResponseEntity.ok(microsoftAuthService.handleCallback(code, state));
  }

  private String oauthError(String error, String errorDescription) {
    if (errorDescription == null || errorDescription.isBlank()) {
      return error;
    }
    return error + " - " + errorDescription;
  }
}
