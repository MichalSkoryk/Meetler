package com.skoryk.projects.meetler.availability.dto;

import com.skoryk.projects.meetler.availability.model.AvailabilityBlockStatus;
import com.skoryk.projects.meetler.availability.model.ResolvedAvailabilitySource;
import com.skoryk.projects.meetler.calendar.CalendarProvider;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResolvedAvailabilityWindowResponse {

  private OffsetDateTime startsAt;
  private OffsetDateTime endsAt;
  private AvailabilityBlockStatus status;
  private ResolvedAvailabilitySource source;
  private UUID sourceBlockId;
  private UUID sourceCalendarId;
  private String sourceCalendarName;
  private CalendarProvider sourceCalendarProvider;
  private String note;
}
