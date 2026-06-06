package com.skoryk.projects.meetler.calendar.external;

import com.skoryk.projects.meetler.calendar.external.dto.ExternalCalendarAccountResponse;
import com.skoryk.projects.meetler.calendar.external.oauth.GoogleCalendarOAuthService;
import com.skoryk.projects.meetler.user.AppUser;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ExternalCalendarOAuthController implements ExternalCalendarOAuthApi {

  private final GoogleCalendarOAuthService googleCalendarOAuthService;

  @Override
  public ResponseEntity<Void> connectGoogle(AppUser user) {
    URI authorizationUri = googleCalendarOAuthService.buildAuthorizationUri(user);
    return ResponseEntity.status(302).location(authorizationUri).build();
  }

  @Override
  public ResponseEntity<ExternalCalendarAccountResponse> googleCallback(String code, String state) {
    return ResponseEntity.ok(googleCalendarOAuthService.handleCallback(code, state));
  }
}
