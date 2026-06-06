package com.skoryk.projects.meetler.auth;

import com.skoryk.projects.meetler.auth.dto.AuthResponse;
import com.skoryk.projects.meetler.auth.dto.LoginRequest;
import com.skoryk.projects.meetler.auth.dto.RefreshTokenRequest;
import com.skoryk.projects.meetler.auth.dto.RegisterRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Authentication")
@RequestMapping("/api/auth")
public interface AuthApi {

  @Operation(
      summary = "Register with email and password",
      description =
          "Creates a new local account and returns access and refresh tokens for the new user.")
  @PostMapping("/register")
  ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request);

  @Operation(
      summary = "Log in with email and password",
      description = "Authenticates a local account and returns fresh access and refresh tokens.")
  @PostMapping("/login")
  ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request);

  @Operation(
      summary = "Refresh access token",
      description = "Uses a valid refresh token to issue a new authentication token response.")
  @PostMapping("/refresh")
  ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request);

  @Operation(
      summary = "Log out",
      description = "Revokes the supplied refresh token so it can no longer be used.")
  @PostMapping("/logout")
  ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request);

  @Operation(
      summary = "Start Google login",
      description = "Redirects the browser to Google's OAuth consent screen for application login.")
  @GetMapping("/google/login")
  ResponseEntity<Void> googleLogin();

  @Operation(
      summary = "Handle Google login callback",
      description =
          "Completes Google OAuth login after Google redirects back with an authorization code.")
  @GetMapping("/google/callback")
  ResponseEntity<AuthResponse> googleCallback(
      @RequestParam(required = false) String code,
      @RequestParam String state,
      @RequestParam(required = false) String error,
      @RequestParam(name = "error_description", required = false) String errorDescription);

  @Operation(
      summary = "Start Microsoft login",
      description =
          "Redirects the browser to Microsoft's OAuth consent screen for application login.")
  @GetMapping("/microsoft/login")
  ResponseEntity<Void> microsoftLogin();

  @Operation(
      summary = "Handle Microsoft login callback",
      description =
          "Completes Microsoft OAuth login after Microsoft redirects back with an authorization code.")
  @GetMapping("/microsoft/callback")
  ResponseEntity<AuthResponse> microsoftCallback(
      @RequestParam(required = false) String code,
      @RequestParam String state,
      @RequestParam(required = false) String error,
      @RequestParam(name = "error_description", required = false) String errorDescription);
}
