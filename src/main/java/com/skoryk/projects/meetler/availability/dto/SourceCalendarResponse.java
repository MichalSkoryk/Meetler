package com.skoryk.projects.meetler.availability.dto;

import com.skoryk.projects.meetler.calendar.CalendarProvider;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SourceCalendarResponse {

  private UUID id;
  private UUID calendarId;
  private String calendarName;
  private CalendarProvider provider;
  private boolean includeBusyEvents;
  private OffsetDateTime createdAt;
}

