package com.skoryk.projects.meetler.calendar.external.google;

import com.skoryk.projects.meetler.calendar.external.ExternalCalendarAccount;
import com.skoryk.projects.meetler.calendar.external.ExternalCalendarAccountRepository;
import com.skoryk.projects.meetler.calendar.external.TokenEncryptionService;
import com.skoryk.projects.meetler.calendar.external.oauth.GoogleOAuthProperties;
import com.skoryk.projects.meetler.calendar.external.oauth.GoogleTokenResponse;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class GoogleCalendarTokenService {

  private final ExternalCalendarAccountRepository accountRepository;
  private final TokenEncryptionService tokenEncryptionService;
  private final GoogleOAuthProperties googleOAuthProperties;
  private final RestClient restClient = RestClient.create();

  public String activeAccessToken(ExternalCalendarAccount account) {
    if (account.getExpiresAt() == null
        || account.getExpiresAt().isAfter(OffsetDateTime.now().plusMinutes(1))) {
      return tokenEncryptionService.decrypt(account.getAccessTokenEncrypted());
    }

    if (account.getRefreshTokenEncrypted() == null) {
      throw new IllegalArgumentException(
          "Google access token expired and no refresh token is stored");
    }

    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("client_id", googleOAuthProperties.getClientId());
    form.add("client_secret", googleOAuthProperties.getClientSecret());
    form.add("refresh_token", tokenEncryptionService.decrypt(account.getRefreshTokenEncrypted()));
    form.add("grant_type", "refresh_token");

    GoogleTokenResponse tokenResponse =
        restClient
            .post()
            .uri(googleOAuthProperties.getTokenUri())
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(form)
            .retrieve()
            .body(GoogleTokenResponse.class);

    if (tokenResponse == null || tokenResponse.getAccessToken() == null) {
      throw new IllegalArgumentException("Google token refresh failed");
    }

    account.setAccessTokenEncrypted(tokenEncryptionService.encrypt(tokenResponse.getAccessToken()));
    account.setTokenType(tokenResponse.getTokenType());
    account.setExpiresAt(OffsetDateTime.now().plusSeconds(tokenResponse.getExpiresIn()));
    accountRepository.save(account);
    return tokenResponse.getAccessToken();
  }
}
