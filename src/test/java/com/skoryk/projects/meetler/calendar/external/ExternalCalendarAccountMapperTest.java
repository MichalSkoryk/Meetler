package com.skoryk.projects.meetler.calendar.external;

import static org.assertj.core.api.Assertions.assertThat;

import com.skoryk.projects.meetler.calendar.CalendarProvider;
import com.skoryk.projects.meetler.calendar.external.dto.ExternalCalendarAccountResponse;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class ExternalCalendarAccountMapperTest {

  private final ExternalCalendarAccountMapper mapper =
      Mappers.getMapper(ExternalCalendarAccountMapper.class);

  @Test
  void mapsOnlyPublicAccountFields() {
    UUID accountId = UUID.randomUUID();
    OffsetDateTime expiresAt = OffsetDateTime.parse("2026-09-01T12:00:00+02:00");
    ExternalCalendarAccount account =
        ExternalCalendarAccount.builder()
            .id(accountId)
            .provider(CalendarProvider.GOOGLE)
            .externalAccountId("google-account")
            .accountEmail("user@example.com")
            .scopes("calendar.readonly")
            .accessTokenEncrypted("secret-access-token")
            .refreshTokenEncrypted("secret-refresh-token")
            .tokenType("Bearer")
            .expiresAt(expiresAt)
            .build();

    ExternalCalendarAccountResponse response = mapper.toResponse(account);

    assertThat(response.getId()).isEqualTo(accountId);
    assertThat(response.getProvider()).isEqualTo(CalendarProvider.GOOGLE);
    assertThat(response.getExternalAccountId()).isEqualTo("google-account");
    assertThat(response.getAccountEmail()).isEqualTo("user@example.com");
    assertThat(response.getScopes()).isEqualTo("calendar.readonly");
    assertThat(response.getTokenType()).isEqualTo("Bearer");
    assertThat(response.getExpiresAt()).isEqualTo(expiresAt);
    assertThat(
            Arrays.stream(ExternalCalendarAccountResponse.class.getDeclaredFields())
                .map(field -> field.getName()))
        .doesNotContain("accessTokenEncrypted", "refreshTokenEncrypted", "user");
  }
}
