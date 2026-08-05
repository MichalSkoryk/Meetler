package com.skoryk.projects.meetler.calendar.external.dto;

import com.skoryk.projects.meetler.calendar.CalendarProvider;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ExternalCalendarAccountResponse {

  private UUID id;
  private CalendarProvider provider;
  private String externalAccountId;
  private String accountEmail;
  private String scopes;
  private String tokenType;
  private OffsetDateTime expiresAt;
  private OffsetDateTime lastSyncedAt;
  private OffsetDateTime revokedAt;
  private OffsetDateTime createdAt;
  private OffsetDateTime updatedAt;
}
