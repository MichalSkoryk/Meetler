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
  private static final String HOSTED_FRONTEND = "https://meetler-frontend-dev.onrender.com";

  private final OAuthStateService service = new OAuthStateService(SECRET, HOSTED_FRONTEND);

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
  void purposeStateCanCarryReturnUrlFromConfiguredOrigin() {
    String returnUrl = HOSTED_FRONTEND + "/oauth/callback?returnTo=%2Fapp%2Fcalendar";

    String state = service.createState("AUTH", "GOOGLE", returnUrl);

    assertThat(service.validatePurposeStateWithReturnUrl(state, "AUTH", "GOOGLE"))
        .isEqualTo(returnUrl);
  }

  @Test
  void configuredOriginUsesNormalizedDefaultPort() {
    OAuthStateService configuredService =
        new OAuthStateService(SECRET, "https://meetler-frontend-dev.onrender.com:443");

    String state =
        configuredService.createState("AUTH", "MICROSOFT", HOSTED_FRONTEND + "/oauth/callback");

    assertThat(configuredService.validatePurposeStateWithReturnUrl(state, "AUTH", "MICROSOFT"))
        .isEqualTo(HOSTED_FRONTEND + "/oauth/callback");
  }

  @Test
  void purposeStateRejectsExternalReturnUrl() {
    assertThatThrownBy(() -> service.createState("AUTH", "GOOGLE", "https://evil.example/oauth"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Invalid OAuth return URL");
  }

  @Test
  void purposeStateRejectsLookalikeConfiguredOrigin() {
    assertThatThrownBy(
            () ->
                service.createState(
                    "AUTH",
                    "GOOGLE",
                    "https://meetler-frontend-dev.onrender.com.evil.example/oauth/callback"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Invalid OAuth return URL");
  }

  @Test
  void purposeStateRejectsConfiguredHostOnAnotherPort() {
    assertThatThrownBy(
            () ->
                service.createState(
                    "AUTH",
                    "GOOGLE",
                    "https://meetler-frontend-dev.onrender.com:8443/oauth/callback"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Invalid OAuth return URL");
  }

  @Test
  void purposeStateRejectsAbsoluteWebUrlWithoutHost() {
    assertThatThrownBy(() -> service.createState("AUTH", "GOOGLE", "https:/oauth/callback"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Invalid OAuth return URL");
  }

  @Test
  void invalidConfiguredOriginFailsFast() {
    assertThatThrownBy(() -> new OAuthStateService(SECRET, HOSTED_FRONTEND + "/oauth/callback"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Invalid configured OAuth return origin");
  }
}
