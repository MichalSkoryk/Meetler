package com.skoryk.projects.meetler.group.event.dto;

import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ExportGroupEventResponse {

  private UUID groupEventId;
  private UUID externalCalendarAccountId;
  private String provider;
  private String externalCalendarId;
  private String externalEventId;
  private String status;
  private OffsetDateTime lastSyncedAt;
}
