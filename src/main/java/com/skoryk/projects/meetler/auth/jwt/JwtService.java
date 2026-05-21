package com.skoryk.projects.meetler.auth.jwt;

import com.skoryk.projects.meetler.user.AppUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import java.util.UUID;
import java.util.function.Function;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class JwtService {

  private final String SECRET;

  public JwtService(@Value("${jwt.secret}") String secret) {
    this.SECRET = secret;
  }

  public String getUserIdStr(String token) {
    return extractClaim(token, Claims::getSubject);
  }

  public String extractName(String token) {
    return extractClaim(token, claims -> claims.get("name", String.class));
  }

  public <T> T extractClaim(String token, Function<Claims, T> resolver) {
    Claims claims =
        Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token).getPayload();
    return resolver.apply(claims);
  }

  public String generateToken(UUID userId, String name) {

    return Jwts.builder()
        .subject(userId.toString())
        .claim("name", name)
        .issuedAt(new Date(System.currentTimeMillis()))
        .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 24))
        .signWith(getSigningKey(), SignatureAlgorithm.HS256)
        .compact();
  }

  public boolean isTokenValid(String token, AppUser userDetails) {
    final UUID userId = UUID.fromString(getUserIdStr(token));

    if (isTokenExpired(token)) {
      log.warn("Token expired: {}", token);
      return false;
    }
    return userId.equals(userDetails.getId());
  }

  private boolean isTokenExpired(String token) {
    return extractExpiration(token).before(new Date());
  }

  private Date extractExpiration(String token) {
    return extractClaim(token, Claims::getExpiration);
  }

  private SecretKey getSigningKey() {
    byte[] keyBytes = Decoders.BASE64.decode(SECRET);
    return Keys.hmacShaKeyFor(keyBytes);
  }
}
