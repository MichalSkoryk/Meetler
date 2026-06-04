package com.skoryk.projects.meetler.calendar.external;

import com.skoryk.projects.meetler.calendar.external.dto.ExternalCalendarAccountResponse;
import com.skoryk.projects.meetler.calendar.external.oauth.GoogleCalendarOAuthService;
import com.skoryk.projects.meetler.user.AppUser;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "External Calendar OAuth")
@RestController
@RequestMapping("/api/external-calendars")
@RequiredArgsConstructor
public class ExternalCalendarOAuthController {

  private final GoogleCalendarOAuthService googleCalendarOAuthService;

  @GetMapping("/google/connect")
  public ResponseEntity<Void> connectGoogle(@AuthenticationPrincipal AppUser user) {
    URI authorizationUri = googleCalendarOAuthService.buildAuthorizationUri(user);
    return ResponseEntity.status(302).location(authorizationUri).build();
  }

  @GetMapping("/google/callback")
  public ResponseEntity<ExternalCalendarAccountResponse> googleCallback(
      @RequestParam String code, @RequestParam String state) {
    return ResponseEntity.ok(googleCalendarOAuthService.handleCallback(code, state));
  }
}
