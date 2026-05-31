package com.skoryk.projects.meetler.calendar.external.dto;

import java.time.OffsetDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ExternalCalendarTokens {

  private String accessToken;
  private String refreshToken;
  private String tokenType;
  private OffsetDateTime expiresAt;
}
