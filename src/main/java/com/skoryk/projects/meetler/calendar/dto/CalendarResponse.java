package com.skoryk.projects.meetler.calendar.dto;

import com.skoryk.projects.meetler.calendar.CalendarProvider;
import com.skoryk.projects.meetler.calendar.CalendarSynchronizationType;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CalendarResponse {
  private UUID id;
  private String name;
  private CalendarProvider provider;
  private String externalId;
  private String color;
  private boolean isEditable;
  private boolean isActive;
  private CalendarSynchronizationType syncDirection;
  private OffsetDateTime createdAt;
  private OffsetDateTime updatedAt;
}
