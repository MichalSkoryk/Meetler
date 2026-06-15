package com.skoryk.projects.meetler.calendar.external.google;

import com.skoryk.projects.meetler.calendar.Calendar;
import com.skoryk.projects.meetler.calendar.CalendarProvider;
import com.skoryk.projects.meetler.calendar.CalendarRepository;
import com.skoryk.projects.meetler.calendar.CalendarSynchronizationType;
import com.skoryk.projects.meetler.calendar.event.CalendarEvent;
import com.skoryk.projects.meetler.calendar.event.CalendarEventRepository;
import com.skoryk.projects.meetler.calendar.external.ExternalCalendarAccount;
import com.skoryk.projects.meetler.calendar.external.ExternalCalendarAccountRepository;
import com.skoryk.projects.meetler.calendar.external.dto.ExternalCalendarImportResponse;
import com.skoryk.projects.meetler.calendar.external.google.GoogleCalendarEventsResponse.GoogleCalendarEventDate;
import com.skoryk.projects.meetler.calendar.external.google.GoogleCalendarEventsResponse.GoogleCalendarEventResponse;
import com.skoryk.projects.meetler.calendar.external.google.GoogleCalendarListResponse.GoogleCalendarResponse;
import com.skoryk.projects.meetler.user.AppUser;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class GoogleCalendarImportService {

  private static final String GOOGLE_CALENDAR_API = "https://www.googleapis.com/calendar/v3";
  private static final int DEFAULT_IMPORT_WINDOW_MONTHS = 6;

  private final ExternalCalendarAccountRepository accountRepository;
  private final CalendarRepository calendarRepository;
  private final CalendarEventRepository eventRepository;
  private final GoogleCalendarTokenService tokenService;
  private final RestClient restClient = RestClient.create();

  @Transactional
  public ExternalCalendarImportResponse importCalendars(
      AppUser user, OffsetDateTime from, OffsetDateTime to) {
    CalendarImportRange range = resolveImportRange(from, to);

    List<ExternalCalendarAccount> accounts =
        accountRepository.findByUserAndProviderAndRevokedAtIsNull(user, CalendarProvider.GOOGLE);

    int calendarsImported = 0;
    int eventsImported = 0;
    for (ExternalCalendarAccount account : accounts) {
      String accessToken = tokenService.activeAccessToken(account);
      GoogleCalendarListResponse calendarList = fetchCalendars(accessToken);

      if (calendarList.getItems() == null) {
        account.setLastSyncedAt(OffsetDateTime.now());
        continue;
      }

      for (GoogleCalendarResponse googleCalendar : calendarList.getItems()) {
        Calendar calendar = upsertCalendar(user, account, googleCalendar);
        calendarsImported++;
        eventsImported +=
            importEvents(calendar, accessToken, googleCalendar.getId(), range.from(), range.to());
      }

      account.setLastSyncedAt(OffsetDateTime.now());
    }

    return ExternalCalendarImportResponse.builder()
        .accountsSynced(accounts.size())
        .calendarsImported(calendarsImported)
        .eventsImported(eventsImported)
        .from(range.from())
        .to(range.to())
        .build();
  }

  private GoogleCalendarListResponse fetchCalendars(String accessToken) {
    return restClient
        .get()
        .uri(GOOGLE_CALENDAR_API + "/users/me/calendarList")
        .headers(headers -> headers.setBearerAuth(accessToken))
        .retrieve()
        .body(GoogleCalendarListResponse.class);
  }

  private int importEvents(
      Calendar calendar,
      String accessToken,
      String calendarExternalId,
      OffsetDateTime from,
      OffsetDateTime to) {
    GoogleCalendarEventsResponse response =
        restClient
            .get()
            .uri(
                uriBuilder ->
                    uriBuilder
                        .scheme("https")
                        .host("www.googleapis.com")
                        .pathSegment("calendar", "v3", "calendars", calendarExternalId, "events")
                        .queryParam("timeMin", from.toInstant().toString())
                        .queryParam("timeMax", to.toInstant().toString())
                        .queryParam("singleEvents", "true")
                        .queryParam("orderBy", "startTime")
                        .build())
            .headers(headers -> headers.setBearerAuth(accessToken))
            .retrieve()
            .body(GoogleCalendarEventsResponse.class);

    if (response == null || response.getItems() == null) {
      return 0;
    }

    int imported = 0;
    for (GoogleCalendarEventResponse googleEvent : response.getItems()) {
      if (googleEvent.getStart() == null || googleEvent.getEnd() == null) {
        continue;
      }
      OffsetDateTime startsAt = parseGoogleDate(googleEvent.getStart());
      OffsetDateTime endsAt = parseGoogleDate(googleEvent.getEnd());
      if (startsAt == null || endsAt == null || !endsAt.isAfter(startsAt)) {
        continue;
      }

      CalendarEvent event =
          eventRepository
              .findByCalendarAndExternalId(calendar, googleEvent.getId())
              .orElseGet(
                  () ->
                      CalendarEvent.builder()
                          .calendar(calendar)
                          .externalId(googleEvent.getId())
                          .build());

      event.setTitle(googleEvent.getSummary());
      event.setStartsAt(startsAt);
      event.setEndsAt(endsAt);
      event.setBusy(!"transparent".equalsIgnoreCase(googleEvent.getTransparency()));
      eventRepository.save(event);
      imported++;
    }

    return imported;
  }

  private Calendar upsertCalendar(
      AppUser user, ExternalCalendarAccount account, GoogleCalendarResponse googleCalendar) {
    Calendar calendar =
        calendarRepository
            .findByUserAndProviderAndExternalId(
                user, CalendarProvider.GOOGLE, googleCalendar.getId())
            .orElseGet(
                () ->
                    Calendar.builder()
                        .user(user)
                        .provider(CalendarProvider.GOOGLE)
                        .externalId(googleCalendar.getId())
                        .isEditable(false)
                        .isActive(true)
                        .syncDirection(CalendarSynchronizationType.FROM_PROVIDER)
                        .build());

    calendar.setName(
        googleCalendar.getSummary() == null ? "Google Calendar" : googleCalendar.getSummary());
    calendar.setColor(googleCalendar.getBackgroundColor());
    calendar.setExternalCalendarAccount(account);
    calendar.setActive(
        Boolean.TRUE.equals(googleCalendar.getSelected()) || googleCalendar.getSelected() == null);
    return calendarRepository.save(calendar);
  }

  private OffsetDateTime parseGoogleDate(GoogleCalendarEventDate date) {
    if (date.getDateTime() != null && !date.getDateTime().isBlank()) {
      return OffsetDateTime.parse(date.getDateTime());
    }
    if (date.getDate() != null && !date.getDate().isBlank()) {
      ZoneId zoneId =
          date.getTimeZone() == null ? ZoneId.systemDefault() : ZoneId.of(date.getTimeZone());
      return LocalDate.parse(date.getDate()).atStartOfDay(zoneId).toOffsetDateTime();
    }
    return null;
  }

  private CalendarImportRange resolveImportRange(
      OffsetDateTime requestedFrom, OffsetDateTime requestedTo) {
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
    OffsetDateTime from =
        requestedFrom == null || requestedFrom.isBefore(now) ? now : requestedFrom;
    OffsetDateTime to =
        requestedTo == null ? from.plusMonths(DEFAULT_IMPORT_WINDOW_MONTHS) : requestedTo;

    if (!to.isAfter(from)) {
      throw new IllegalArgumentException("Calendar import range end must be after start");
    }
    return new CalendarImportRange(from, to);
  }

  private record CalendarImportRange(OffsetDateTime from, OffsetDateTime to) {}
}
