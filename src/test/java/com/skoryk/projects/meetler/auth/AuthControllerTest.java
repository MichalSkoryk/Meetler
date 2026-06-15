package com.skoryk.projects.meetler.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.skoryk.projects.meetler.auth.google.GoogleAuthService;
import com.skoryk.projects.meetler.auth.guest.GuestLoginService;
import com.skoryk.projects.meetler.auth.microsoft.MicrosoftAuthService;
import com.skoryk.projects.meetler.auth.password.PasswordResetService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

class AuthControllerTest {

  @Test
  void openPasswordResetPageRedirectsToFrontendWithToken() {
    AuthController controller =
        new AuthController(
            mock(AuthService.class),
            mock(GuestLoginService.class),
            mock(PasswordResetService.class),
            mock(GoogleAuthService.class),
            mock(MicrosoftAuthService.class));
    ReflectionTestUtils.setField(
        controller, "passwordResetFrontendUrl", "http://localhost:8081/reset");

    ResponseEntity<Void> response = controller.openPasswordResetPage("raw-token");

    assertThat(response.getStatusCode().value()).isEqualTo(302);
    assertThat(response.getHeaders().getLocation())
        .hasToString("http://localhost:8081/reset?resetPassword=true&token=raw-token");
  }
}
