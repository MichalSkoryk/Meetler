package com.skoryk.projects.meetler.calendar.external.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class OAuthStateServiceTest {

  private static final String SECRET =
      Base64.getEncoder()
          .encodeToString("01234567890123456789012345678901".getBytes(StandardCharsets.UTF_8));

  private final OAuthStateService service = new OAuthStateService(SECRET);

  @Test
  void purposeStateCanCarryReturnUrl() {
    String state = service.createState("AUTH", "GOOGLE", "http://localhost:3000/oauth");

    String returnUrl = service.validatePurposeStateWithReturnUrl(state, "AUTH", "GOOGLE");

    assertThat(returnUrl).isEqualTo("http://localhost:3000/oauth");
  }

  @Test
  void purposeStateCanCarryMobileDeepLinkReturnUrl() {
    String state = service.createState("AUTH", "GOOGLE", "meetler://oauth/callback");

    String returnUrl = service.validatePurposeStateWithReturnUrl(state, "AUTH", "GOOGLE");

    assertThat(returnUrl).isEqualTo("meetler://oauth/callback");
  }

  @Test
  void purposeStateRejectsExternalReturnUrl() {
    assertThatThrownBy(() -> service.createState("AUTH", "GOOGLE", "https://evil.example/oauth"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Invalid OAuth return URL");
  }
}
