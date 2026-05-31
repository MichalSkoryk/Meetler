package com.skoryk.projects.meetler.calendar.dto;

import com.skoryk.projects.meetler.calendar.CalendarProvider;
import com.skoryk.projects.meetler.calendar.CalendarSynchronizationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateCalendarRequest {
  @NotBlank
  @Size(max = 255)
  private String name;

  @NotNull private CalendarProvider provider;

  @Size(max = 255)
  private String externalId;

  @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "must be a hex color like #4f46e5")
  private String color;

  private boolean isEditable;

  @NotNull private CalendarSynchronizationType syncDirection;
}
