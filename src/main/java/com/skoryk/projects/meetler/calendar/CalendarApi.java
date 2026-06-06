package com.skoryk.projects.meetler.calendar;

import com.skoryk.projects.meetler.calendar.dto.CalendarResponse;
import com.skoryk.projects.meetler.calendar.dto.CreateCalendarRequest;
import com.skoryk.projects.meetler.calendar.dto.UpdateCalendarRequest;
import com.skoryk.projects.meetler.user.AppUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Calendars")
@RequestMapping("/api/calendars")
public interface CalendarApi {

  @Operation(
      summary = "Create a calendar",
      description =
          "Creates a calendar owned by the authenticated user. Internal calendars are editable; external calendars are synced from a provider.")
  @PostMapping
  ResponseEntity<CalendarResponse> createCalendar(
      @Valid @RequestBody CreateCalendarRequest request, @AuthenticationPrincipal AppUser user);

  @Operation(
      summary = "List calendars",
      description = "Returns calendars owned by the authenticated user.")
  @GetMapping
  ResponseEntity<List<CalendarResponse>> getUserCalendars(@AuthenticationPrincipal AppUser user);

  @Operation(
      summary = "Update a calendar",
      description =
          "Updates editable calendar metadata for a calendar owned by the authenticated user.")
  @PatchMapping("/{id}")
  ResponseEntity<CalendarResponse> updateCalendar(
      @PathVariable UUID id,
      @Valid @RequestBody UpdateCalendarRequest request,
      @AuthenticationPrincipal AppUser user);

  @Operation(
      summary = "Delete a calendar",
      description = "Deletes a calendar owned by the authenticated user.")
  @DeleteMapping("/{id}")
  ResponseEntity<Void> deleteCalendar(@PathVariable UUID id, @AuthenticationPrincipal AppUser user);
}
