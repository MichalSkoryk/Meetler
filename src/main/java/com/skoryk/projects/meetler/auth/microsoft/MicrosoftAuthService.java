package com.skoryk.projects.meetler.auth.microsoft;

import com.skoryk.projects.meetler.auth.AuthService;
import com.skoryk.projects.meetler.auth.OAuthLoginResult;
import com.skoryk.projects.meetler.auth.OAuthResponseMode;
import com.skoryk.projects.meetler.calendar.external.oauth.OAuthStateService;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class MicrosoftAuthService {

  private static final String PROVIDER = "MICROSOFT";
  private static final String PURPOSE = "MEETLER_MICROSOFT_AUTH";

  private final MicrosoftAuthProperties properties;
  private final OAuthStateService stateService;
  private final AuthService authService;
  private final RestClient restClient = RestClient.create();

  public URI buildAuthorizationUri(String returnUrl) {
    return buildAuthorizationUri(returnUrl, null);
  }

  public URI buildAuthorizationUri(String returnUrl, String responseMode) {
    requireConfigured();
    OAuthResponseMode mode = OAuthResponseMode.from(responseMode);
    if (mode == OAuthResponseMode.MOBILE_CODE && (returnUrl == null || returnUrl.isBlank())) {
      throw new IllegalArgumentException("Mobile OAuth requires a return URL");
    }

    String state = stateService.createState(PURPOSE, PROVIDER, returnUrl, mode.value());

    return UriComponentsBuilder.fromUriString(properties.getAuthorizationUri())
        .queryParam("client_id", properties.getClientId())
        .queryParam("redirect_uri", properties.getRedirectUri())
        .queryParam("response_type", "code")
        .queryParam("response_mode", "query")
        .queryParam("scope", String.join(" ", properties.getScopes()))
        .queryParam("state", state)
        .build()
        .encode()
        .toUri();
  }

  public OAuthLoginResult handleCallbackWithRedirect(String code, String state) {
    requireConfigured();
    OAuthStateService.OAuthPurposeState oauthState =
        stateService.validatePurposeState(state, PURPOSE, PROVIDER);

    MicrosoftTokenResponse tokenResponse = exchangeCodeForTokens(code);
    MicrosoftUserInfoResponse userInfo = fetchUserInfo(tokenResponse.getAccessToken());

    String email = userInfo.resolveEmail();
    if (email == null || email.isBlank()) {
      throw new IllegalArgumentException("Microsoft account email was not returned");
    }

    if (userInfo.getSub() == null || userInfo.getSub().isBlank()) {
      throw new IllegalArgumentException("Microsoft user id was not returned");
    }

    var user =
        authService.authenticateMicrosoftIdentity(userInfo.getSub(), email, userInfo.getName());
    return new OAuthLoginResult(
        user, oauthState.returnUrl(), OAuthResponseMode.from(oauthState.responseMode()));
  }

  private MicrosoftTokenResponse exchangeCodeForTokens(String code) {
    MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
    body.add("client_id", properties.getClientId());
    body.add("client_secret", properties.getClientSecret());
    body.add("code", code);
    body.add("grant_type", "authorization_code");
    body.add("redirect_uri", properties.getRedirectUri());

    return restClient
        .post()
        .uri(properties.getTokenUri())
        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        .body(body)
        .retrieve()
        .body(MicrosoftTokenResponse.class);
  }

  private MicrosoftUserInfoResponse fetchUserInfo(String accessToken) {
    return restClient
        .get()
        .uri(properties.getUserInfoUri())
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
        .retrieve()
        .body(MicrosoftUserInfoResponse.class);
  }

  private void requireConfigured() {
    if (isBlank(properties.getClientId())
        || isBlank(properties.getClientSecret())
        || isBlank(properties.getRedirectUri())) {
      throw new IllegalStateException("Microsoft authentication is not configured");
    }
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
