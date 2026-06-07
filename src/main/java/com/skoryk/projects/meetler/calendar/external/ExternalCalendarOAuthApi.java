package com.skoryk.projects.meetler.calendar.external;

import com.skoryk.projects.meetler.calendar.external.dto.ExternalCalendarImportResponse;
import com.skoryk.projects.meetler.calendar.external.dto.ExternalCalendarAccountResponse;
import com.skoryk.projects.meetler.user.AppUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.OffsetDateTime;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "External Calendar OAuth")
@RequestMapping("/api/external-calendars")
public interface ExternalCalendarOAuthApi {

  @Operation(
      summary = "Connect Google Calendar",
      description =
          "Redirects the authenticated user to Google OAuth consent for calendar synchronization access.")
  @GetMapping("/google/connect")
  ResponseEntity<Void> connectGoogle(@AuthenticationPrincipal AppUser user);

  @Operation(
      summary = "Handle Google Calendar callback",
      description =
          "Completes the Google Calendar OAuth connection and stores the external calendar account tokens.")
  @GetMapping("/google/callback")
  ResponseEntity<ExternalCalendarAccountResponse> googleCallback(
      @RequestParam String code, @RequestParam String state);

  @Operation(
      summary = "Import Google calendars and events",
      description =
          "Imports calendars and busy/free events from connected Google Calendar accounts for the requested date-time range.")
  @PostMapping("/google/import")
  ResponseEntity<ExternalCalendarImportResponse> importGoogleCalendars(
      @RequestParam OffsetDateTime from,
      @RequestParam OffsetDateTime to,
      @AuthenticationPrincipal AppUser user);
}
