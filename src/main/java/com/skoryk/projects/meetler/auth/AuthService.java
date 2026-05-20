package com.skoryk.projects.meetler.auth;

import com.skoryk.projects.meetler.auth.dto.AuthResponse;
import com.skoryk.projects.meetler.auth.dto.LoginRequest;
import com.skoryk.projects.meetler.auth.dto.RegisterRequest;
import com.skoryk.projects.meetler.auth.jwt.JwtService;
import com.skoryk.projects.meetler.auth.token.RefreshTokenService;
import com.skoryk.projects.meetler.user.AppUser;
import com.skoryk.projects.meetler.user.AppUserRepository;
import com.skoryk.projects.meetler.user.AppUserRole;
import com.skoryk.projects.meetler.user.AuthProvider;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

  private final AppUserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final RefreshTokenService refreshTokenService;

  public AuthResponse register(RegisterRequest request) {
    if (userRepository.findByEmail(request.getEmail().toLowerCase()).isPresent()) {
      throw new IllegalArgumentException("Email already registered");
    }

    AppUser user =
        AppUser.builder()
            .email(request.getEmail().toLowerCase())
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .role(AppUserRole.USER)
            .authProvider(AuthProvider.INTERNAL)
            .createdAt(OffsetDateTime.now())
            .build();

    userRepository.save(user);

    String accessToken = jwtService.generateToken(user.getId(), user.getEmail());
    String refreshToken = refreshTokenService.createRefreshToken(user);

    return new AuthResponse(accessToken, refreshToken);
  }

  public AuthResponse login(LoginRequest request) {
    AppUser user =
        userRepository
            .findByEmail(request.getEmail().toLowerCase())
            .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

    if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
      throw new IllegalArgumentException("Invalid credentials");
    }

    String accessToken = jwtService.generateToken(user.getId(), user.getEmail());
    String refreshToken = refreshTokenService.rotateRefreshToken(user);

    return new AuthResponse(accessToken, refreshToken);
  }

  public AuthResponse refreshToken(String refreshToken) {
    AppUser user = refreshTokenService.validateAndRotate(refreshToken);

    String newAccessToken = jwtService.generateToken(user.getId(), user.getEmail());
    String newRefreshToken = refreshTokenService.rotateRefreshToken(user);

    return new AuthResponse(newAccessToken, newRefreshToken);
  }

  public void logout(String refreshToken) {
    refreshTokenService.revoke(refreshToken);
  }
}
