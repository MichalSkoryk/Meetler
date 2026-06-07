package com.skoryk.projects.meetler.calendar.external.dto;

import java.time.OffsetDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ExternalCalendarImportResponse {

  private int accountsSynced;
  private int calendarsImported;
  private int eventsImported;
  private OffsetDateTime from;
  private OffsetDateTime to;
}
