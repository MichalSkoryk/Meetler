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
  private final CalendarMapper calendarMapper;

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
    return calendarMapper.toResponse(calendar);
  }

  public List<CalendarResponse> getUserCalendars(AppUser user) {
    return calendarRepository.findByUser(user).stream().map(calendarMapper::toResponse).toList();
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

    return calendarMapper.toResponse(calendar);
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
}
