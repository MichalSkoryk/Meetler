package com.skoryk.projects.meetler.availability.dto;

import com.skoryk.projects.meetler.availability.AvailabilityBlockStatus;
import com.skoryk.projects.meetler.availability.AvailabilityRecurrenceFrequency;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RecurringAvailabilityBlockResponse {

  private UUID id;
  private AvailabilityRecurrenceFrequency frequency;
  private Integer intervalCount;
  private Integer occurrenceCount;
  private DayOfWeek dayOfWeek;
  private Integer dayOfMonth;
  private Integer monthOfYear;
  private LocalTime startTime;
  private LocalTime endTime;
  private AvailabilityBlockStatus status;
  private String note;
  private LocalDate startsOn;
  private LocalDate endsOn;
  private OffsetDateTime createdAt;
  private OffsetDateTime updatedAt;
}
