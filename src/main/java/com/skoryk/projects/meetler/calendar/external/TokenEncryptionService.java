package com.skoryk.projects.meetler.calendar.external;

import jakarta.annotation.PostConstruct;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TokenEncryptionService {

  private static final String CIPHER = "AES/GCM/NoPadding";
  private static final int IV_BYTES = 12;
  private static final int TAG_BITS = 128;

  private final SecureRandom secureRandom = new SecureRandom();
  private final String base64Key;
  private SecretKey key;

  public TokenEncryptionService(
      @Value("${external-calendar.token-encryption-key}") String base64Key) {
    this.base64Key = base64Key;
  }

  @PostConstruct
  void initializeKey() {
    byte[] keyBytes = Base64.getDecoder().decode(base64Key);
    if (keyBytes.length != 16 && keyBytes.length != 24 && keyBytes.length != 32) {
      throw new IllegalStateException(
          "external-calendar.token-encryption-key must decode to 16, 24, or 32 bytes");
    }
    this.key = new SecretKeySpec(keyBytes, "AES");
  }

  public String encrypt(String rawToken) {
    if (rawToken == null || rawToken.isBlank()) {
      return null;
    }

    try {
      byte[] iv = new byte[IV_BYTES];
      secureRandom.nextBytes(iv);

      Cipher cipher = Cipher.getInstance(CIPHER);
      cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
      byte[] encrypted = cipher.doFinal(rawToken.getBytes(StandardCharsets.UTF_8));

      ByteBuffer payload = ByteBuffer.allocate(iv.length + encrypted.length);
      payload.put(iv);
      payload.put(encrypted);

      return Base64.getEncoder().encodeToString(payload.array());
    } catch (GeneralSecurityException ex) {
      throw new IllegalStateException("Could not encrypt external calendar token", ex);
    }
  }

  public String decrypt(String encryptedToken) {
    if (encryptedToken == null || encryptedToken.isBlank()) {
      return null;
    }

    try {
      byte[] payload = Base64.getDecoder().decode(encryptedToken);
      ByteBuffer buffer = ByteBuffer.wrap(payload);

      byte[] iv = new byte[IV_BYTES];
      buffer.get(iv);

      byte[] encrypted = new byte[buffer.remaining()];
      buffer.get(encrypted);

      Cipher cipher = Cipher.getInstance(CIPHER);
      cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
      return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
    } catch (GeneralSecurityException | IllegalArgumentException ex) {
      throw new IllegalStateException("Could not decrypt external calendar token", ex);
    }
  }
}
