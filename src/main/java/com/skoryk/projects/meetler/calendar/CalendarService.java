package com.skoryk.projects.meetler.calendar;

import com.skoryk.projects.meetler.calendar.dto.CalendarResponse;
import com.skoryk.projects.meetler.calendar.dto.CreateCalendarRequest;
import com.skoryk.projects.meetler.calendar.dto.UpdateCalendarRequest;
import com.skoryk.projects.meetler.user.AppUser;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CalendarService {

  private final CalendarRepository calendarRepository;

  public CalendarResponse createCalendar(CreateCalendarRequest request, AppUser user) {
    Calendar calendar =
        Calendar.builder()
            .user(user)
            .provider(request.getProvider())
            .externalId(request.getExternalId())
            .name(request.getName())
            .color(request.getColor())
            .isEditable(request.isEditable())
            .isActive(true)
            .syncDirection(request.getSyncDirection())
            .createdAt(OffsetDateTime.now())
            .updatedAt(OffsetDateTime.now())
            .build();

    calendarRepository.save(calendar);
    return toResponse(calendar);
  }

  public List<CalendarResponse> getUserCalendars(AppUser user) {
    return calendarRepository.findByUser(user).stream().map(this::toResponse).toList();
  }

  public CalendarResponse updateCalendar(UUID id, UpdateCalendarRequest request, AppUser user) {
    Calendar calendar =
        calendarRepository
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Calendar not found"));

    if (!calendar.getUser().getId().equals(user.getId())) {
      throw new IllegalArgumentException("Not allowed");
    }

    if (request.getName() != null) calendar.setName(request.getName());
    if (request.getColor() != null) calendar.setColor(request.getColor());
    if (request.getIsActive() != null) calendar.setActive(request.getIsActive());
    if (request.getIsEditable() != null) calendar.setEditable(request.getIsEditable());
    if (request.getSyncDirection() != null) calendar.setSyncDirection(request.getSyncDirection());

    calendar.setUpdatedAt(OffsetDateTime.now());
    calendarRepository.save(calendar);

    return toResponse(calendar);
  }

  public void deleteCalendar(UUID id, AppUser user) {
    Calendar calendar =
        calendarRepository
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Calendar not found"));

    if (!calendar.getUser().getId().equals(user.getId())) {
      throw new IllegalArgumentException("Not allowed");
    }

    calendarRepository.delete(calendar);
  }

  private CalendarResponse toResponse(Calendar calendar) {
    return CalendarResponse.builder()
        .id(calendar.getId())
        .name(calendar.getName())
        .provider(calendar.getProvider())
        .externalId(calendar.getExternalId())
        .color(calendar.getColor())
        .isEditable(calendar.isEditable())
        .isActive(calendar.isActive())
        .syncDirection(calendar.getSyncDirection())
        .createdAt(calendar.getCreatedAt())
        .updatedAt(calendar.getUpdatedAt())
        .build();
  }
}
