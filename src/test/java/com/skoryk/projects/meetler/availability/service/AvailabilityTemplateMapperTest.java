package com.skoryk.projects.meetler.availability.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.skoryk.projects.meetler.availability.dto.AvailabilityBlockResponse;
import com.skoryk.projects.meetler.availability.dto.AvailabilityTemplateResponse;
import com.skoryk.projects.meetler.availability.dto.RecurringAvailabilityBlockResponse;
import com.skoryk.projects.meetler.availability.dto.SourceCalendarResponse;
import com.skoryk.projects.meetler.availability.model.AvailabilityBlockSource;
import com.skoryk.projects.meetler.availability.model.AvailabilityBlockStatus;
import com.skoryk.projects.meetler.availability.model.AvailabilityRecurrenceFrequency;
import com.skoryk.projects.meetler.availability.model.AvailabilityTemplate;
import com.skoryk.projects.meetler.availability.model.AvailabilityTemplateBlock;
import com.skoryk.projects.meetler.availability.model.AvailabilityTemplateRecurringBlock;
import com.skoryk.projects.meetler.availability.model.AvailabilityTemplateSourceCalendar;
import com.skoryk.projects.meetler.calendar.Calendar;
import com.skoryk.projects.meetler.calendar.CalendarProvider;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class AvailabilityTemplateMapperTest {

  private final AvailabilityTemplateMapper mapper =
      Mappers.getMapper(AvailabilityTemplateMapper.class);

  @Test
  void mapsTemplateToResponse() {
    OffsetDateTime createdAt = OffsetDateTime.parse("2026-08-01T12:00:00+02:00");
    AvailabilityTemplate template =
        AvailabilityTemplate.builder()
            .id(UUID.randomUUID())
            .name("Weekdays")
            .timezone("Europe/Warsaw")
            .isDefault(true)
            .defaultAvailabilityStatus(AvailabilityBlockStatus.BUSY)
            .createdAt(createdAt)
            .updatedAt(createdAt.plusDays(1))
            .build();

    AvailabilityTemplateResponse response = mapper.toTemplateResponse(template);

    assertThat(response.getId()).isEqualTo(template.getId());
    assertThat(response.getName()).isEqualTo("Weekdays");
    assertThat(response.getTimezone()).isEqualTo("Europe/Warsaw");
    assertThat(response.isDefault()).isTrue();
    assertThat(response.getDefaultAvailabilityStatus()).isEqualTo(AvailabilityBlockStatus.BUSY);
    assertThat(response.getCreatedAt()).isEqualTo(createdAt);
    assertThat(response.getUpdatedAt()).isEqualTo(createdAt.plusDays(1));
  }

  @Test
  void mapsOneTimeBlockToResponse() {
    OffsetDateTime startsAt = OffsetDateTime.parse("2026-08-10T18:00:00+02:00");
    AvailabilityTemplateBlock block =
        AvailabilityTemplateBlock.builder()
            .id(UUID.randomUUID())
            .startsAt(startsAt)
            .endsAt(startsAt.plusHours(2))
            .status(AvailabilityBlockStatus.AVAILABLE)
            .source(AvailabilityBlockSource.MANUAL)
            .note("Rehearsal")
            .createdAt(startsAt.minusDays(1))
            .updatedAt(startsAt.minusHours(1))
            .build();

    AvailabilityBlockResponse response = mapper.toBlockResponse(block);

    assertThat(response.getId()).isEqualTo(block.getId());
    assertThat(response.getStartsAt()).isEqualTo(startsAt);
    assertThat(response.getEndsAt()).isEqualTo(startsAt.plusHours(2));
    assertThat(response.getStatus()).isEqualTo(AvailabilityBlockStatus.AVAILABLE);
    assertThat(response.getSource()).isEqualTo(AvailabilityBlockSource.MANUAL);
    assertThat(response.getNote()).isEqualTo("Rehearsal");
    assertThat(response.getCreatedAt()).isEqualTo(startsAt.minusDays(1));
    assertThat(response.getUpdatedAt()).isEqualTo(startsAt.minusHours(1));
  }

  @Test
  void mapsRecurringBlockToResponse() {
    AvailabilityTemplateRecurringBlock block =
        AvailabilityTemplateRecurringBlock.builder()
            .id(UUID.randomUUID())
            .frequency(AvailabilityRecurrenceFrequency.WEEKLY)
            .intervalCount(2)
            .occurrenceCount(5)
            .dayOfWeek(DayOfWeek.MONDAY)
            .startTime(LocalTime.of(18, 0))
            .endTime(LocalTime.of(20, 0))
            .status(AvailabilityBlockStatus.BUSY)
            .note("Weekly rehearsal")
            .startsOn(LocalDate.of(2026, 8, 10))
            .endsOn(LocalDate.of(2026, 12, 31))
            .createdAt(OffsetDateTime.parse("2026-08-01T12:00:00+02:00"))
            .updatedAt(OffsetDateTime.parse("2026-08-02T12:00:00+02:00"))
            .build();

    RecurringAvailabilityBlockResponse response = mapper.toRecurringBlockResponse(block);

    assertThat(response.getId()).isEqualTo(block.getId());
    assertThat(response.getFrequency()).isEqualTo(AvailabilityRecurrenceFrequency.WEEKLY);
    assertThat(response.getIntervalCount()).isEqualTo(2);
    assertThat(response.getOccurrenceCount()).isEqualTo(5);
    assertThat(response.getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
    assertThat(response.getStartTime()).isEqualTo(LocalTime.of(18, 0));
    assertThat(response.getEndTime()).isEqualTo(LocalTime.of(20, 0));
    assertThat(response.getStatus()).isEqualTo(AvailabilityBlockStatus.BUSY);
    assertThat(response.getNote()).isEqualTo("Weekly rehearsal");
    assertThat(response.getStartsOn()).isEqualTo(LocalDate.of(2026, 8, 10));
    assertThat(response.getEndsOn()).isEqualTo(LocalDate.of(2026, 12, 31));
  }

  @Test
  void mapsSourceCalendarAndNestedCalendarProperties() {
    UUID sourceId = UUID.randomUUID();
    UUID calendarId = UUID.randomUUID();
    OffsetDateTime createdAt = OffsetDateTime.parse("2026-08-05T12:00:00+02:00");
    Calendar calendar =
        Calendar.builder().id(calendarId).name("Work").provider(CalendarProvider.GOOGLE).build();
    AvailabilityTemplateSourceCalendar source =
        AvailabilityTemplateSourceCalendar.builder()
            .id(sourceId)
            .calendar(calendar)
            .includeBusyEvents(true)
            .createdAt(createdAt)
            .build();

    SourceCalendarResponse response = mapper.toSourceCalendarResponse(source);

    assertThat(response.getId()).isEqualTo(sourceId);
    assertThat(response.getCalendarId()).isEqualTo(calendarId);
    assertThat(response.getCalendarName()).isEqualTo("Work");
    assertThat(response.getProvider()).isEqualTo(CalendarProvider.GOOGLE);
    assertThat(response.isIncludeBusyEvents()).isTrue();
    assertThat(response.getCreatedAt()).isEqualTo(createdAt);
  }
}
