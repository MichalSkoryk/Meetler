package com.skoryk.projects.meetler.calendar.external.oauth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.net.URI;
import java.time.Duration;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class OAuthStateService {

  private static final Duration STATE_TTL = Duration.ofMinutes(10);

  private final SecretKey signingKey;
  private final Set<Origin> allowedReturnOrigins;

  public OAuthStateService(
      @Value("${jwt.secret}") String jwtSecret,
      @Value("${auth.oauth.allowed-return-origins:}") String allowedReturnOrigins) {
    this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
    this.allowedReturnOrigins = parseAllowedOrigins(allowedReturnOrigins);
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

  public String createState(String purpose, String provider, String returnUrl) {
    return createState(purpose, provider, returnUrl, null);
  }

  public String createState(
      String purpose, String provider, String returnUrl, String responseMode) {
    Date now = new Date();
    Date expiresAt = new Date(now.getTime() + STATE_TTL.toMillis());
    String normalizedReturnUrl = normalizeReturnUrl(returnUrl);

    var builder =
        Jwts.builder()
            .subject(purpose)
            .claim("provider", provider)
            .issuedAt(now)
            .expiration(expiresAt)
            .signWith(signingKey);

    if (normalizedReturnUrl != null) {
      builder.claim("returnUrl", normalizedReturnUrl);
    }
    if (responseMode != null && !responseMode.isBlank()) {
      builder.claim("responseMode", responseMode);
    }

    return builder.compact();
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

  public String validatePurposeStateWithReturnUrl(
      String state, String expectedPurpose, String expectedProvider) {
    return validatePurposeState(state, expectedPurpose, expectedProvider).returnUrl();
  }

  public OAuthPurposeState validatePurposeState(
      String state, String expectedPurpose, String expectedProvider) {
    Claims claims =
        Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(state).getPayload();

    String provider = claims.get("provider", String.class);
    if (!expectedProvider.equals(provider) || !expectedPurpose.equals(claims.getSubject())) {
      throw new IllegalArgumentException("Invalid OAuth state");
    }

    return new OAuthPurposeState(
        normalizeReturnUrl(claims.get("returnUrl", String.class)),
        claims.get("responseMode", String.class));
  }

  public String validateReturnUrl(String returnUrl) {
    return normalizeReturnUrl(returnUrl);
  }

  private String normalizeReturnUrl(String returnUrl) {
    if (returnUrl == null || returnUrl.isBlank()) {
      return null;
    }

    URI uri = URI.create(returnUrl);
    if (uri.isAbsolute()) {
      String scheme = uri.getScheme();
      String host = uri.getHost();
      if ("meetler".equalsIgnoreCase(scheme)) {
        return uri.toString();
      }

      boolean webUrl = "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
      if (webUrl
          && host != null
          && uri.getUserInfo() == null
          && (isLocalHost(host) || allowedReturnOrigins.contains(Origin.from(uri)))) {
        return uri.toString();
      }
      throw new IllegalArgumentException("Invalid OAuth return URL");
    }

    if (returnUrl.startsWith("/") && !returnUrl.startsWith("//")) {
      return returnUrl;
    }

    throw new IllegalArgumentException("Invalid OAuth return URL");
  }

  private Set<Origin> parseAllowedOrigins(String configuredOrigins) {
    return Arrays.stream(configuredOrigins.split(","))
        .map(String::trim)
        .filter(origin -> !origin.isEmpty())
        .map(this::parseAllowedOrigin)
        .collect(Collectors.toUnmodifiableSet());
  }

  private Origin parseAllowedOrigin(String configuredOrigin) {
    URI uri = URI.create(configuredOrigin);
    String scheme = uri.getScheme();
    boolean webOrigin = "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
    boolean rootPath =
        uri.getPath() == null || uri.getPath().isEmpty() || "/".equals(uri.getPath());

    if (!uri.isAbsolute()
        || uri.isOpaque()
        || !webOrigin
        || uri.getHost() == null
        || uri.getUserInfo() != null
        || !rootPath
        || uri.getQuery() != null
        || uri.getFragment() != null) {
      throw new IllegalArgumentException("Invalid configured OAuth return origin");
    }

    return Origin.from(uri);
  }

  private boolean isLocalHost(String host) {
    if (host == null) {
      return false;
    }
    String normalizedHost = host.replace("[", "").replace("]", "");
    return "localhost".equalsIgnoreCase(normalizedHost)
        || "127.0.0.1".equals(normalizedHost)
        || "::1".equals(normalizedHost);
  }

  private record Origin(String scheme, String host, int port) {

    private static Origin from(URI uri) {
      String scheme = uri.getScheme().toLowerCase(Locale.ROOT);
      String host = uri.getHost().toLowerCase(Locale.ROOT);
      int port = uri.getPort();
      if (port == -1) {
        port = "https".equals(scheme) ? 443 : 80;
      }
      return new Origin(scheme, host, port);
    }
  }

  public record OAuthUserState(UUID userId, String returnUrl) {}

  public record OAuthPurposeState(String returnUrl, String responseMode) {}
}
