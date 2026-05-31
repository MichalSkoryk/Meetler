package com.skoryk.projects.meetler.calendar.external.dto;

import com.skoryk.projects.meetler.calendar.CalendarProvider;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import lombok.Data;

@Data
public class StoreExternalCalendarAccountRequest {

  @NotNull private CalendarProvider provider;

  @NotBlank private String externalAccountId;

  @Email private String accountEmail;

  private String scopes;

  @NotBlank private String accessToken;

  private String refreshToken;

  private String tokenType;

  private OffsetDateTime expiresAt;
}
