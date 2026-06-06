package com.skoryk.projects.meetler.calendar;

import com.skoryk.projects.meetler.calendar.dto.CalendarResponse;
import com.skoryk.projects.meetler.calendar.dto.CreateCalendarRequest;
import com.skoryk.projects.meetler.calendar.dto.UpdateCalendarRequest;
import com.skoryk.projects.meetler.user.AppUser;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CalendarController implements CalendarApi {

  private final CalendarService calendarService;

  @Override
  public ResponseEntity<CalendarResponse> createCalendar(
      CreateCalendarRequest request, AppUser user) {
    return ResponseEntity.ok(calendarService.createCalendar(request, user));
  }

  @Override
  public ResponseEntity<List<CalendarResponse>> getUserCalendars(AppUser user) {
    return ResponseEntity.ok(calendarService.getUserCalendars(user));
  }

  @Override
  public ResponseEntity<CalendarResponse> updateCalendar(
      UUID id, UpdateCalendarRequest request, AppUser user) {
    return ResponseEntity.ok(calendarService.updateCalendar(id, request, user));
  }

  @Override
  public ResponseEntity<Void> deleteCalendar(UUID id, AppUser user) {
    calendarService.deleteCalendar(id, user);
    return ResponseEntity.noContent().build();
  }
}
