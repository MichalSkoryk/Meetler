package com.skoryk.projects.meetler.auth.google;

import com.skoryk.projects.meetler.auth.AuthService;
import com.skoryk.projects.meetler.auth.dto.AuthResponse;
import com.skoryk.projects.meetler.calendar.external.oauth.GoogleTokenResponse;
import com.skoryk.projects.meetler.calendar.external.oauth.GoogleUserInfoResponse;
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
public class GoogleAuthService {

  private static final String PROVIDER = "GOOGLE";
  private static final String PURPOSE = "MEETLER_GOOGLE_AUTH";

  private final GoogleAuthProperties properties;
  private final OAuthStateService stateService;
  private final AuthService authService;
  private final RestClient restClient = RestClient.create();

  public URI buildAuthorizationUri() {
    requireConfigured();

    String state = stateService.createState(PURPOSE, PROVIDER);

    return UriComponentsBuilder.fromUriString(properties.getAuthorizationUri())
        .queryParam("client_id", properties.getClientId())
        .queryParam("redirect_uri", properties.getRedirectUri())
        .queryParam("response_type", "code")
        .queryParam("scope", String.join(" ", properties.getScopes()))
        .queryParam("state", state)
        .build()
        .encode()
        .toUri();
  }

  public AuthResponse handleCallback(String code, String state) {
    requireConfigured();
    stateService.validateState(state, PURPOSE, PROVIDER);

    GoogleTokenResponse tokenResponse = exchangeCodeForTokens(code);
    GoogleUserInfoResponse userInfo = fetchUserInfo(tokenResponse.getAccessToken());

    if (userInfo.getEmail() == null || userInfo.getEmail().isBlank()) {
      throw new IllegalArgumentException("Google email was not returned");
    }

    if (userInfo.getSub() == null || userInfo.getSub().isBlank()) {
      throw new IllegalArgumentException("Google user id was not returned");
    }

    if (Boolean.FALSE.equals(userInfo.getEmailVerified())) {
      throw new IllegalArgumentException("Google email is not verified");
    }

    return authService.authenticateGoogleUser(
        userInfo.getSub(), userInfo.getEmail(), userInfo.getName());
  }

  private GoogleTokenResponse exchangeCodeForTokens(String code) {
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
        .body(GoogleTokenResponse.class);
  }

  private GoogleUserInfoResponse fetchUserInfo(String accessToken) {
    return restClient
        .get()
        .uri(properties.getUserInfoUri())
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
        .retrieve()
        .body(GoogleUserInfoResponse.class);
  }

  private void requireConfigured() {
    if (isBlank(properties.getClientId())
        || isBlank(properties.getClientSecret())
        || isBlank(properties.getRedirectUri())) {
      throw new IllegalStateException("Google authentication is not configured");
    }
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
