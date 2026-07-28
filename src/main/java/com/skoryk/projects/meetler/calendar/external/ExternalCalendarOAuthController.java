package com.skoryk.projects.meetler.calendar.external;

import com.skoryk.projects.meetler.calendar.external.dto.ExternalCalendarAuthorizationResponse;
import com.skoryk.projects.meetler.calendar.external.dto.ExternalCalendarImportResponse;
import com.skoryk.projects.meetler.calendar.external.google.GoogleCalendarImportService;
import com.skoryk.projects.meetler.calendar.external.oauth.GoogleCalendarOAuthService;
import com.skoryk.projects.meetler.calendar.external.oauth.GoogleCalendarOAuthService.GoogleCalendarOAuthResult;
import com.skoryk.projects.meetler.user.AppUser;
import java.net.URI;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ExternalCalendarOAuthController implements ExternalCalendarOAuthApi {

  private final GoogleCalendarOAuthService googleCalendarOAuthService;
  private final GoogleCalendarImportService googleCalendarImportService;

  @Override
  public ResponseEntity<ExternalCalendarAuthorizationResponse> googleAuthorizationUrl(
      AppUser user, String returnUrl) {
    URI authorizationUri = googleCalendarOAuthService.buildAuthorizationUri(user, returnUrl);
    return ResponseEntity.ok(
        new ExternalCalendarAuthorizationResponse(authorizationUri.toString()));
  }

  @Override
  public ResponseEntity<Void> connectGoogle(AppUser user, String returnUrl) {
    URI authorizationUri = googleCalendarOAuthService.buildAuthorizationUri(user, returnUrl);
    return ResponseEntity.status(302).location(authorizationUri).build();
  }

  @Override
  public ResponseEntity<Void> googleCallback(String code, String state) {
    GoogleCalendarOAuthResult result =
        googleCalendarOAuthService.handleCallbackWithReturnUrl(code, state);
    URI returnUri = URI.create(result.returnUrl() == null ? "/" : result.returnUrl());
    return ResponseEntity.status(302).location(returnUri).build();
  }

  @Override
  public ResponseEntity<ExternalCalendarImportResponse> importGoogleCalendars(
      OffsetDateTime from, OffsetDateTime to, AppUser user) {
    return ResponseEntity.ok(googleCalendarImportService.importCalendars(user, from, to));
  }
}
