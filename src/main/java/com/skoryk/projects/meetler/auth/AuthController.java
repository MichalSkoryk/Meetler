package com.skoryk.projects.meetler.auth;

import com.skoryk.projects.meetler.auth.dto.AuthResponse;
import com.skoryk.projects.meetler.auth.dto.LoginRequest;
import com.skoryk.projects.meetler.auth.dto.RefreshTokenRequest;
import com.skoryk.projects.meetler.auth.dto.RegisterRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;

  @PostMapping("/register")
  public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
    AuthResponse response = authService.register(request);
    return ResponseEntity.ok(response);
  }

  @PostMapping("/login")
  public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
    AuthResponse response = authService.login(request);
    return ResponseEntity.ok(response);
  }

  @PostMapping("/refresh")
  public ResponseEntity<AuthResponse> refresh(@RequestBody RefreshTokenRequest request) {
    AuthResponse response = authService.refreshToken(request.getRefreshToken());
    return ResponseEntity.ok(response);
  }

  @PostMapping("/logout")
  public ResponseEntity<Void> logout(@RequestBody RefreshTokenRequest request) {
    authService.logout(request.getRefreshToken());
    return ResponseEntity.noContent().build();
  }
}
