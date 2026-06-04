package com.skoryk.projects.meetler.calendar.external.oauth;

import com.skoryk.projects.meetler.calendar.CalendarProvider;
import com.skoryk.projects.meetler.calendar.external.ExternalCalendarAccountService;
import com.skoryk.projects.meetler.calendar.external.dto.ExternalCalendarAccountResponse;
import com.skoryk.projects.meetler.calendar.external.dto.StoreExternalCalendarAccountRequest;
import com.skoryk.projects.meetler.user.AppUser;
import com.skoryk.projects.meetler.user.AppUserRepository;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.UUID;
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
public class GoogleCalendarOAuthService {

  private static final String PROVIDER = "GOOGLE";

  private final GoogleOAuthProperties properties;
  private final OAuthStateService stateService;
  private final AppUserRepository userRepository;
  private final ExternalCalendarAccountService externalCalendarAccountService;
  private final RestClient restClient = RestClient.create();

  public URI buildAuthorizationUri(AppUser user) {
    requireConfigured();

    String state = stateService.createState(user.getId(), PROVIDER);

    return UriComponentsBuilder.fromUriString(properties.getAuthorizationUri())
        .queryParam("client_id", properties.getClientId())
        .queryParam("redirect_uri", properties.getRedirectUri())
        .queryParam("response_type", "code")
        .queryParam("scope", String.join(" ", properties.getScopes()))
        .queryParam("access_type", "offline")
        .queryParam("prompt", "consent")
        .queryParam("state", state)
        .build()
        .encode()
        .toUri();
  }

  public ExternalCalendarAccountResponse handleCallback(String code, String state) {
    requireConfigured();

    UUID userId = stateService.validateState(state, PROVIDER);
    AppUser user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("OAuth user not found"));

    GoogleTokenResponse tokenResponse = exchangeCodeForTokens(code);
    GoogleUserInfoResponse userInfo = fetchUserInfo(tokenResponse.getAccessToken());

    if (userInfo.getSub() == null || userInfo.getSub().isBlank()) {
      throw new IllegalArgumentException("Google account id was not returned");
    }

    StoreExternalCalendarAccountRequest request = new StoreExternalCalendarAccountRequest();
    request.setProvider(CalendarProvider.GOOGLE);
    request.setExternalAccountId(userInfo.getSub());
    request.setAccountEmail(userInfo.getEmail());
    request.setScopes(tokenResponse.getScope());
    request.setAccessToken(tokenResponse.getAccessToken());
    request.setRefreshToken(tokenResponse.getRefreshToken());
    request.setTokenType(tokenResponse.getTokenType());
    request.setExpiresAt(expiresAt(tokenResponse.getExpiresIn()));

    return externalCalendarAccountService.storeOrUpdateAccount(user, request);
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

  private OffsetDateTime expiresAt(Long expiresInSeconds) {
    if (expiresInSeconds == null) {
      return null;
    }
    return OffsetDateTime.now().plusSeconds(expiresInSeconds);
  }

  private void requireConfigured() {
    if (isBlank(properties.getClientId())
        || isBlank(properties.getClientSecret())
        || isBlank(properties.getRedirectUri())) {
      throw new IllegalStateException("Google OAuth is not configured");
    }
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
