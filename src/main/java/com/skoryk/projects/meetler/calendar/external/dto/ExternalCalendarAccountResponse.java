package com.skoryk.projects.meetler.calendar.external.dto;

import com.skoryk.projects.meetler.calendar.CalendarProvider;
import com.skoryk.projects.meetler.calendar.external.ExternalCalendarAccount;
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

  public static ExternalCalendarAccountResponse from(ExternalCalendarAccount account) {
    return ExternalCalendarAccountResponse.builder()
        .id(account.getId())
        .provider(account.getProvider())
        .externalAccountId(account.getExternalAccountId())
        .accountEmail(account.getAccountEmail())
        .scopes(account.getScopes())
        .tokenType(account.getTokenType())
        .expiresAt(account.getExpiresAt())
        .lastSyncedAt(account.getLastSyncedAt())
        .revokedAt(account.getRevokedAt())
        .createdAt(account.getCreatedAt())
        .updatedAt(account.getUpdatedAt())
        .build();
  }
}
