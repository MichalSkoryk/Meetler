package com.skoryk.projects.meetler.availability.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Data;

@Data
public class AddSourceCalendarRequest {

  @NotNull private UUID calendarId;

  private boolean includeBusyEvents = true;
}

