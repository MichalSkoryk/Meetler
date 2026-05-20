package com.skoryk.projects.meetler.security;

import com.skoryk.projects.meetler.auth.dto.AppUserDetails;
import com.skoryk.projects.meetler.auth.jwt.JwtService;
import com.skoryk.projects.meetler.user.AppUser;
import com.skoryk.projects.meetler.user.AppUserRepository;
import com.skoryk.projects.meetler.user.AppUserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtService jwtService;
  private final AppUserRepository userRepository;
  private final AppUserService appUserService;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String authHeader = request.getHeader("Authorization");

    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      filterChain.doFilter(request, response);
      return;
    }

    String token = authHeader.substring(7);

    // Extract userId from JWT
    String userIdStr = jwtService.getUserIdStr(token);
    if (userIdStr == null) {
      filterChain.doFilter(request, response);
      return;
    }

    UUID userId = UUID.fromString(userIdStr);

    // Only authenticate if not already authenticated
    if (SecurityContextHolder.getContext().getAuthentication() == null) {

      AppUser user = userRepository.findById(userId).orElse(null);

      if (user != null && jwtService.isTokenValid(token, user)) {

        AppUserDetails appUserDetails = new AppUserDetails(user);
        UsernamePasswordAuthenticationToken auth =
            new UsernamePasswordAuthenticationToken(
                user, null, appUserDetails.getAuthorities() // if you map roles to authorities
                );

        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContextHolder.getContext().setAuthentication(auth);
      }
    }

    filterChain.doFilter(request, response);
  }
}
