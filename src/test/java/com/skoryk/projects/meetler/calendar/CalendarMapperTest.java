package com.skoryk.projects.meetler.calendar;

import static org.assertj.core.api.Assertions.assertThat;

import com.skoryk.projects.meetler.calendar.dto.CalendarResponse;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class CalendarMapperTest {

  private final CalendarMapper mapper = Mappers.getMapper(CalendarMapper.class);

  @Test
  void mapsCalendarToResponse() {
    OffsetDateTime createdAt = OffsetDateTime.parse("2026-08-01T12:00:00+02:00");
    OffsetDateTime updatedAt = OffsetDateTime.parse("2026-08-02T12:00:00+02:00");
    Calendar calendar =
        Calendar.builder()
            .id(UUID.randomUUID())
            .name("Work")
            .provider(CalendarProvider.GOOGLE)
            .externalId("external-id")
            .color("#112233")
            .isEditable(false)
            .isActive(true)
            .syncDirection(CalendarSynchronizationType.FROM_PROVIDER)
            .createdAt(createdAt)
            .updatedAt(updatedAt)
            .build();

    CalendarResponse response = mapper.toResponse(calendar);

    assertThat(response.getId()).isEqualTo(calendar.getId());
    assertThat(response.getName()).isEqualTo("Work");
    assertThat(response.getProvider()).isEqualTo(CalendarProvider.GOOGLE);
    assertThat(response.getExternalId()).isEqualTo("external-id");
    assertThat(response.getColor()).isEqualTo("#112233");
    assertThat(response.isEditable()).isFalse();
    assertThat(response.isActive()).isTrue();
    assertThat(response.getSyncDirection()).isEqualTo(CalendarSynchronizationType.FROM_PROVIDER);
    assertThat(response.getCreatedAt()).isEqualTo(createdAt);
    assertThat(response.getUpdatedAt()).isEqualTo(updatedAt);
  }
}
