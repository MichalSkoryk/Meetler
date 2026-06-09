package com.skoryk.projects.meetler.email.resend;

import com.skoryk.projects.meetler.email.EmailService;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResendEmailService implements EmailService {

  private final RestClient restClient = RestClient.create();

  @Value("${email.from:Meetler <no-reply@localhost>}")
  private String from;

  @Value("${email.resend.api-key:}")
  private String apiKey;

  @Value("${email.resend.api-url:https://api.resend.com}")
  private String apiUrl;

  @Override
  public void sendGuestLoginLink(
      String recipientEmail, String loginLink, OffsetDateTime expiresAt) {
    if (apiKey == null || apiKey.isBlank()) {
      log.info(
          "Resend API key is not configured. Guest login link for {}: {}",
          recipientEmail,
          loginLink);
      return;
    }

    send(
        recipientEmail,
        "Your Meetler guest login link",
        guestLoginPlainText(loginLink, expiresAt),
        guestLoginHtml(loginLink, expiresAt));
  }

  @Override
  public void sendPasswordResetLink(
      String recipientEmail, String resetLink, OffsetDateTime expiresAt) {
    if (apiKey == null || apiKey.isBlank()) {
      log.info(
          "Resend API key is not configured. Password reset link for {}: {}",
          recipientEmail,
          resetLink);
      return;
    }

    send(
        recipientEmail,
        "Reset your Meetler password",
        passwordResetPlainText(resetLink, expiresAt),
        passwordResetHtml(resetLink, expiresAt));
  }

  private void send(String recipientEmail, String subject, String text, String html) {
    restClient
        .post()
        .uri(apiUrl + "/emails")
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
        .body(
            Map.of(
                "from", from, "to", recipientEmail, "subject", subject, "text", text, "html", html))
        .retrieve()
        .toBodilessEntity();
  }

  private String guestLoginPlainText(String loginLink, OffsetDateTime expiresAt) {
    return """
        Open this link to log in to Meetler as a guest:

        %s

        This link expires at %s and can be used only once.
        """
        .formatted(loginLink, format(expiresAt));
  }

  private String guestLoginHtml(String loginLink, OffsetDateTime expiresAt) {
    return """
        <p>Open this link to log in to Meetler as a guest:</p>
        <p><a href="%s">Log in to Meetler</a></p>
        <p>This link expires at %s and can be used only once.</p>
        """
        .formatted(loginLink, format(expiresAt));
  }

  private String passwordResetPlainText(String resetLink, OffsetDateTime expiresAt) {
    return """
        Open this link to reset your Meetler password:

        %s

        This link expires at %s and can be used only once.
        """
        .formatted(resetLink, format(expiresAt));
  }

  private String passwordResetHtml(String resetLink, OffsetDateTime expiresAt) {
    return """
        <p>Open this link to reset your Meetler password:</p>
        <p><a href="%s">Reset password</a></p>
        <p>This link expires at %s and can be used only once.</p>
        """
        .formatted(resetLink, format(expiresAt));
  }

  private String format(OffsetDateTime expiresAt) {
    return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(expiresAt);
  }
}
