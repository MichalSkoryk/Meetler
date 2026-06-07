package com.skoryk.projects.meetler.availability.dto;

import com.skoryk.projects.meetler.availability.model.AvailabilityRecurrenceFrequency;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.DayOfWeek;
import java.time.LocalDate;
import lombok.Data;

@Data
public class ConvertAvailabilityBlockToRecurringRequest {

  @NotNull private AvailabilityRecurrenceFrequency frequency;

  @Min(1)
  private Integer intervalCount = 1;

  @Min(1)
  private Integer occurrenceCount;

  private DayOfWeek dayOfWeek;

  @Min(1)
  @Max(31)
  private Integer dayOfMonth;

  @Min(1)
  @Max(12)
  private Integer monthOfYear;

  private LocalDate startsOn;

  private LocalDate endsOn;

  private boolean deleteOriginalBlock = true;
}

