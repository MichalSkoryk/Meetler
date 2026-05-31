package com.skoryk.projects.meetler.calendar.dto;

import com.skoryk.projects.meetler.calendar.CalendarSynchronizationType;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateCalendarRequest {
  @Size(min = 1, max = 255)
  private String name;

  @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "must be a hex color like #4f46e5")
  private String color;

  private Boolean isActive;
  private Boolean isEditable;
  private CalendarSynchronizationType syncDirection;
}
