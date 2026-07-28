package com.skoryk.projects.meetler.calendar.external;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.skoryk.projects.meetler.calendar.external.dto.ExternalCalendarAuthorizationResponse;
import com.skoryk.projects.meetler.calendar.external.google.GoogleCalendarImportService;
import com.skoryk.projects.meetler.calendar.external.oauth.GoogleCalendarOAuthService;
import com.skoryk.projects.meetler.user.AppUser;
import java.net.URI;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

class ExternalCalendarOAuthControllerTest {

  @Test
  void returnsAuthorizationUrlForAuthenticatedFrontendRequest() {
    GoogleCalendarOAuthService oauthService = mock(GoogleCalendarOAuthService.class);
    ExternalCalendarOAuthController controller =
        new ExternalCalendarOAuthController(oauthService, mock(GoogleCalendarImportService.class));
    AppUser user = mock(AppUser.class);
    String returnUrl = "http://localhost:8082/app/calendar";
    URI authorizationUri = URI.create("https://accounts.google.com/o/oauth2/v2/auth?state=test");
    when(oauthService.buildAuthorizationUri(user, returnUrl)).thenReturn(authorizationUri);

    ResponseEntity<ExternalCalendarAuthorizationResponse> response =
        controller.googleAuthorizationUrl(user, returnUrl);

    assertThat(response.getStatusCode().value()).isEqualTo(200);
    assertThat(response.getBody())
        .isEqualTo(new ExternalCalendarAuthorizationResponse(authorizationUri.toString()));
    verify(oauthService).buildAuthorizationUri(user, returnUrl);
  }
}
