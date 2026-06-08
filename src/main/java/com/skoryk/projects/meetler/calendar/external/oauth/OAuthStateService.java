package com.skoryk.projects.meetler.calendar.external.oauth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.net.URI;
import java.time.Duration;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class OAuthStateService {

  private static final Duration STATE_TTL = Duration.ofMinutes(10);

  private final SecretKey signingKey;

  public OAuthStateService(@Value("${jwt.secret}") String jwtSecret) {
    this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
  }

  public String createState(UUID userId, String provider) {
    return createState(userId, provider, null);
  }

  public String createState(UUID userId, String provider, String returnUrl) {
    Date now = new Date();
    Date expiresAt = new Date(now.getTime() + STATE_TTL.toMillis());
    String normalizedReturnUrl = normalizeReturnUrl(returnUrl);

    var builder =
        Jwts.builder()
            .subject(userId.toString())
            .claim("provider", provider)
            .issuedAt(now)
            .expiration(expiresAt)
            .signWith(signingKey);

    if (normalizedReturnUrl != null) {
      builder.claim("returnUrl", normalizedReturnUrl);
    }

    return builder.compact();
  }

  public String createState(String purpose, String provider) {
    Date now = new Date();
    Date expiresAt = new Date(now.getTime() + STATE_TTL.toMillis());

    return Jwts.builder()
        .subject(purpose)
        .claim("provider", provider)
        .issuedAt(now)
        .expiration(expiresAt)
        .signWith(signingKey)
        .compact();
  }

  public UUID validateState(String state, String expectedProvider) {
    return validateStateWithReturnUrl(state, expectedProvider).userId();
  }

  public OAuthUserState validateStateWithReturnUrl(String state, String expectedProvider) {
    Claims claims =
        Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(state).getPayload();

    String provider = claims.get("provider", String.class);
    if (!expectedProvider.equals(provider)) {
      throw new IllegalArgumentException("Invalid OAuth state");
    }

    return new OAuthUserState(
        UUID.fromString(claims.getSubject()),
        normalizeReturnUrl(claims.get("returnUrl", String.class)));
  }

  public void validateState(String state, String expectedPurpose, String expectedProvider) {
    Claims claims =
        Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(state).getPayload();

    String provider = claims.get("provider", String.class);
    if (!expectedProvider.equals(provider) || !expectedPurpose.equals(claims.getSubject())) {
      throw new IllegalArgumentException("Invalid OAuth state");
    }
  }

  private String normalizeReturnUrl(String returnUrl) {
    if (returnUrl == null || returnUrl.isBlank()) {
      return null;
    }

    URI uri = URI.create(returnUrl);
    if (uri.isAbsolute()) {
      String scheme = uri.getScheme();
      String host = uri.getHost();
      boolean localHost =
          "localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host) || "::1".equals(host);
      if (("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) && localHost) {
        return uri.toString();
      }
      throw new IllegalArgumentException("Invalid OAuth return URL");
    }

    if (returnUrl.startsWith("/") && !returnUrl.startsWith("//")) {
      return returnUrl;
    }

    throw new IllegalArgumentException("Invalid OAuth return URL");
  }

  public record OAuthUserState(UUID userId, String returnUrl) {}
}
