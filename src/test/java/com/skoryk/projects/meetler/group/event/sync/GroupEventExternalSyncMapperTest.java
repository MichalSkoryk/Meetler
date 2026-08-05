package com.skoryk.projects.meetler.group.event.sync;

import static org.assertj.core.api.Assertions.assertThat;

import com.skoryk.projects.meetler.calendar.CalendarProvider;
import com.skoryk.projects.meetler.calendar.external.ExternalCalendarAccount;
import com.skoryk.projects.meetler.group.event.GroupEvent;
import com.skoryk.projects.meetler.group.event.dto.ExportGroupEventResponse;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class GroupEventExternalSyncMapperTest {

  private final GroupEventExternalSyncMapper mapper =
      Mappers.getMapper(GroupEventExternalSyncMapper.class);

  @Test
  void mapsExternalSyncAndNestedIdentifiers() {
    UUID eventId = UUID.randomUUID();
    UUID accountId = UUID.randomUUID();
    OffsetDateTime lastSyncedAt = OffsetDateTime.parse("2026-08-05T18:00:00+02:00");
    GroupEventExternalSync sync =
        GroupEventExternalSync.builder()
            .groupEvent(GroupEvent.builder().id(eventId).build())
            .externalCalendarAccount(ExternalCalendarAccount.builder().id(accountId).build())
            .provider(CalendarProvider.GOOGLE)
            .externalCalendarId("calendar-id")
            .externalEventId("event-id")
            .status(GroupEventExternalSyncStatus.SYNCED)
            .lastSyncedAt(lastSyncedAt)
            .build();

    ExportGroupEventResponse response = mapper.toResponse(sync);

    assertThat(response.getGroupEventId()).isEqualTo(eventId);
    assertThat(response.getExternalCalendarAccountId()).isEqualTo(accountId);
    assertThat(response.getProvider()).isEqualTo("GOOGLE");
    assertThat(response.getExternalCalendarId()).isEqualTo("calendar-id");
    assertThat(response.getExternalEventId()).isEqualTo("event-id");
    assertThat(response.getStatus()).isEqualTo("SYNCED");
    assertThat(response.getLastSyncedAt()).isEqualTo(lastSyncedAt);
  }
}
