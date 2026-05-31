package com.skoryk.projects.meetler.calendar;

import com.skoryk.projects.meetler.calendar.dto.CalendarResponse;
import com.skoryk.projects.meetler.calendar.dto.CreateCalendarRequest;
import com.skoryk.projects.meetler.calendar.dto.UpdateCalendarRequest;
import com.skoryk.projects.meetler.user.AppUser;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Calendar")
@RestController
@RequestMapping("/api/calendars")
@RequiredArgsConstructor
public class CalendarController {

  private final CalendarService calendarService;

  @PostMapping
  public ResponseEntity<CalendarResponse> createCalendar(
      @Valid @RequestBody CreateCalendarRequest request, @AuthenticationPrincipal AppUser user) {
    return ResponseEntity.ok(calendarService.createCalendar(request, user));
  }

  @GetMapping
  public ResponseEntity<List<CalendarResponse>> getUserCalendars(
      @AuthenticationPrincipal AppUser user) {
    return ResponseEntity.ok(calendarService.getUserCalendars(user));
  }

  @PatchMapping("/{id}")
  public ResponseEntity<CalendarResponse> updateCalendar(
      @PathVariable UUID id,
      @Valid @RequestBody UpdateCalendarRequest request,
      @AuthenticationPrincipal AppUser user) {
    return ResponseEntity.ok(calendarService.updateCalendar(id, request, user));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteCalendar(
      @PathVariable UUID id, @AuthenticationPrincipal AppUser user) {
    calendarService.deleteCalendar(id, user);
    return ResponseEntity.noContent().build();
  }
}
