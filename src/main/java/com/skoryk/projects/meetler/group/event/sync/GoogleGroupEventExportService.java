package com.skoryk.projects.meetler.group.event.sync;

import com.skoryk.projects.meetler.calendar.Calendar;
import com.skoryk.projects.meetler.calendar.CalendarProvider;
import com.skoryk.projects.meetler.calendar.CalendarRepository;
import com.skoryk.projects.meetler.calendar.CalendarSynchronizationType;
import com.skoryk.projects.meetler.calendar.external.ExternalCalendarAccount;
import com.skoryk.projects.meetler.calendar.external.ExternalCalendarAccountRepository;
import com.skoryk.projects.meetler.calendar.external.google.GoogleCalendarListResponse;
import com.skoryk.projects.meetler.calendar.external.google.GoogleCalendarListResponse.GoogleCalendarResponse;
import com.skoryk.projects.meetler.calendar.external.google.GoogleCalendarTokenService;
import com.skoryk.projects.meetler.calendar.external.google.GoogleCalendarWriteResponse;
import com.skoryk.projects.meetler.group.Group;
import com.skoryk.projects.meetler.group.GroupRepository;
import com.skoryk.projects.meetler.group.event.GroupEvent;
import com.skoryk.projects.meetler.group.event.GroupEventRepository;
import com.skoryk.projects.meetler.group.event.GroupEventStatus;
import com.skoryk.projects.meetler.group.event.dto.ExportGroupEventResponse;
import com.skoryk.projects.meetler.group.member.GroupPermissionService;
import com.skoryk.projects.meetler.user.AppUser;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleGroupEventExportService {

  private static final String GOOGLE_CALENDAR_API = "https://www.googleapis.com/calendar/v3";
  private static final String MEETLER_CALENDAR_NAME = "Meetler";
  private static final String GOOGLE_CALENDAR_SCOPE = "https://www.googleapis.com/auth/calendar";
  private static final String GOOGLE_CALENDAR_APP_CREATED_SCOPE =
      "https://www.googleapis.com/auth/calendar.app.created";
  private static final DateTimeFormatter GOOGLE_DATE_TIME_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

  private final GroupRepository groupRepository;
  private final GroupEventRepository groupEventRepository;
  private final GroupPermissionService groupPermissionService;
  private final ExternalCalendarAccountRepository externalCalendarAccountRepository;
  private final CalendarRepository calendarRepository;
  private final GroupEventExternalSyncRepository syncRepository;
  private final GoogleCalendarTokenService tokenService;
  private final GroupEventExternalSyncMapper syncMapper;
  private final RestClient restClient = RestClient.create();

  @Transactional
  public ExportGroupEventResponse exportToGoogle(UUID groupId, UUID eventId, AppUser user) {
    Group group =
        groupRepository
            .findById(groupId)
            .orElseThrow(() -> new IllegalArgumentException("Group not found"));
    if (!groupPermissionService.isMember(group, user)) {
      throw new IllegalArgumentException("Not allowed");
    }

    GroupEvent event =
        groupEventRepository
            .findByIdAndGroup(eventId, group)
            .orElseThrow(() -> new IllegalArgumentException("Group event not found"));
    if (event.getStatus() == GroupEventStatus.CANCELLED) {
      throw new IllegalStateException("Cancelled events cannot be exported");
    }
    if (event.getStatus() != GroupEventStatus.CONFIRMED) {
      throw new IllegalStateException("Only confirmed events can be exported");
    }
    validateEventForGoogleExport(event);

    ExternalCalendarAccount account =
        externalCalendarAccountRepository
            .findByUserAndProviderAndRevokedAtIsNull(user, CalendarProvider.GOOGLE)
            .stream()
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("No connected Google account found"));
    requireCalendarWriteScope(account);
    String accessToken = tokenService.activeAccessToken(account);

    GoogleCalendarWriteResponse meetlerCalendar =
        findOrCreateMeetlerCalendar(user, account, accessToken);

    GroupEventExternalSync sync =
        syncRepository
            .findByGroupEventAndExternalCalendarAccountAndProvider(
                event, account, CalendarProvider.GOOGLE)
            .orElse(null);

    GoogleCalendarWriteResponse googleEvent =
        sync == null || sync.getStatus() != GroupEventExternalSyncStatus.SYNCED
            ? createGoogleEvent(meetlerCalendar.getId(), event, accessToken)
            : updateGoogleEvent(
                sync.getExternalCalendarId(), sync.getExternalEventId(), event, accessToken);

    OffsetDateTime now = OffsetDateTime.now();
    if (sync == null) {
      sync =
          GroupEventExternalSync.builder()
              .groupEvent(event)
              .externalCalendarAccount(account)
              .provider(CalendarProvider.GOOGLE)
              .build();
    }
    sync.setExternalCalendarId(meetlerCalendar.getId());
    sync.setExternalEventId(googleEvent.getId());
    sync.setStatus(GroupEventExternalSyncStatus.SYNCED);
    sync.setLastSyncedAt(now);
    sync.setLastError(null);
    return syncMapper.toResponse(syncRepository.save(sync));
  }

  @Transactional
  public void synchronizeExistingGoogleExports(GroupEvent event) {
    List<GroupEventExternalSync> syncs =
        syncRepository.findByGroupEventAndProviderAndStatus(
            event, CalendarProvider.GOOGLE, GroupEventExternalSyncStatus.SYNCED);

    if (event.getStatus() != GroupEventStatus.CONFIRMED) {
      deleteSyncs(syncs);
      return;
    }

    for (GroupEventExternalSync sync : syncs) {
      String accessToken = tokenService.activeAccessToken(sync.getExternalCalendarAccount());
      try {
        GoogleCalendarWriteResponse updated =
            updateGoogleEvent(
                sync.getExternalCalendarId(), sync.getExternalEventId(), event, accessToken);
        sync.setExternalEventId(updated.getId());
        sync.setStatus(GroupEventExternalSyncStatus.SYNCED);
        sync.setLastSyncedAt(OffsetDateTime.now());
        sync.setLastError(null);
      } catch (RuntimeException ex) {
        sync.setStatus(GroupEventExternalSyncStatus.FAILED);
        sync.setLastSyncedAt(OffsetDateTime.now());
        sync.setLastError(truncateError(ex.getMessage()));
      }
      syncRepository.save(sync);
    }
  }

  @Transactional
  public void deleteExistingGoogleExports(GroupEvent event) {
    List<GroupEventExternalSync> syncs =
        syncRepository.findByGroupEventAndProviderAndStatus(
            event, CalendarProvider.GOOGLE, GroupEventExternalSyncStatus.SYNCED);
    deleteSyncs(syncs);
  }

  private GoogleCalendarWriteResponse findOrCreateMeetlerCalendar(
      AppUser user, ExternalCalendarAccount account, String accessToken) {
    Calendar localCalendar =
        calendarRepository
            .findFirstByUserAndProviderAndExternalCalendarAccountAndSyncDirectionAndNameOrderByUpdatedAtDesc(
                user,
                CalendarProvider.GOOGLE,
                account,
                CalendarSynchronizationType.TO_PROVIDER,
                MEETLER_CALENDAR_NAME)
            .filter(
                calendar -> calendar.getExternalId() != null && !calendar.getExternalId().isBlank())
            .orElse(null);

    if (localCalendar != null) {
      return googleCalendar(
          localCalendar.getExternalId(), localCalendar.getName(), localCalendar.getColor());
    }

    Calendar importedMeetlerCalendar =
        calendarRepository
            .findFirstByUserAndProviderAndExternalCalendarAccountAndNameOrderByUpdatedAtDesc(
                user, CalendarProvider.GOOGLE, account, MEETLER_CALENDAR_NAME)
            .filter(
                calendar -> calendar.getExternalId() != null && !calendar.getExternalId().isBlank())
            .orElse(null);
    if (importedMeetlerCalendar != null) {
      importedMeetlerCalendar.setEditable(true);
      importedMeetlerCalendar.setSyncDirection(CalendarSynchronizationType.TO_PROVIDER);
      Calendar saved = calendarRepository.save(importedMeetlerCalendar);
      return googleCalendar(saved.getExternalId(), saved.getName(), saved.getColor());
    }

    if (hasFullCalendarScope(account)) {
      GoogleCalendarWriteResponse existingGoogleCalendar =
          findExistingMeetlerCalendarFromGoogle(user, account, accessToken);
      if (existingGoogleCalendar != null) {
        return existingGoogleCalendar;
      }
    }

    GoogleCalendarWriteResponse created =
        executeGoogleWrite(
            "create Meetler calendar",
            () ->
                restClient
                    .post()
                    .uri(GOOGLE_CALENDAR_API + "/calendars")
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .body(Map.of("summary", MEETLER_CALENDAR_NAME, "timeZone", "UTC"))
                    .retrieve()
                    .body(GoogleCalendarWriteResponse.class));

    if (created == null || created.getId() == null) {
      throw new IllegalStateException("Google calendar creation failed");
    }
    upsertLocalGoogleCalendar(user, account, created.getId(), created.getSummary());
    return created;
  }

  private GoogleCalendarWriteResponse findExistingMeetlerCalendarFromGoogle(
      AppUser user, ExternalCalendarAccount account, String accessToken) {
    GoogleCalendarListResponse calendarList =
        restClient
            .get()
            .uri(GOOGLE_CALENDAR_API + "/users/me/calendarList")
            .headers(headers -> headers.setBearerAuth(accessToken))
            .retrieve()
            .body(GoogleCalendarListResponse.class);

    if (calendarList == null || calendarList.getItems() == null) {
      return null;
    }

    for (GoogleCalendarResponse calendar : calendarList.getItems()) {
      if (MEETLER_CALENDAR_NAME.equalsIgnoreCase(calendar.getSummary())) {
        upsertLocalGoogleCalendar(user, account, calendar.getId(), calendar.getSummary());
        return googleCalendar(
            calendar.getId(), calendar.getSummary(), calendar.getBackgroundColor());
      }
    }
    return null;
  }

  private void requireCalendarWriteScope(ExternalCalendarAccount account) {
    String scopes = account.getScopes();
    if (scopes == null || List.of(scopes.split("\\s+")).stream().noneMatch(this::canExportEvent)) {
      throw new IllegalStateException(
          "Connected Google account is missing Meetler calendar export scope. Reconnect Google Calendar.");
    }
  }

  private boolean canExportEvent(String scope) {
    return GOOGLE_CALENDAR_SCOPE.equals(scope) || GOOGLE_CALENDAR_APP_CREATED_SCOPE.equals(scope);
  }

  private boolean hasFullCalendarScope(ExternalCalendarAccount account) {
    String scopes = account.getScopes();
    return scopes != null
        && List.of(scopes.split("\\s+")).stream().anyMatch(GOOGLE_CALENDAR_SCOPE::equals);
  }

  private void validateEventForGoogleExport(GroupEvent event) {
    if (event.getTitle() == null || event.getTitle().isBlank()) {
      throw new IllegalArgumentException("Event title is required for Google export");
    }
    if (event.getStartsAt() == null || event.getEndsAt() == null) {
      throw new IllegalArgumentException("Event start and end are required for Google export");
    }
    if (!event.getEndsAt().isAfter(event.getStartsAt())) {
      throw new IllegalArgumentException("Event end must be after start for Google export");
    }
  }

  private GoogleCalendarWriteResponse createGoogleEvent(
      String calendarId, GroupEvent event, String accessToken) {
    Map<String, Object> body = toGoogleEventBody(event);
    GoogleCalendarWriteResponse created =
        executeGoogleWrite(
            "create Google event calendarId="
                + calendarId
                + " groupEventId="
                + event.getId()
                + " payload="
                + body,
            () ->
                restClient
                    .post()
                    .uri(
                        uriBuilder ->
                            uriBuilder
                                .scheme("https")
                                .host("www.googleapis.com")
                                .pathSegment("calendar", "v3", "calendars", calendarId, "events")
                                .build())
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .body(body)
                    .retrieve()
                    .body(GoogleCalendarWriteResponse.class));

    if (created == null || created.getId() == null) {
      throw new IllegalStateException("Google event creation failed");
    }
    return created;
  }

  private GoogleCalendarWriteResponse updateGoogleEvent(
      String calendarId, String externalEventId, GroupEvent event, String accessToken) {
    Map<String, Object> body = toGoogleEventBody(event);
    GoogleCalendarWriteResponse updated =
        executeGoogleWrite(
            "update Google event calendarId="
                + calendarId
                + " externalEventId="
                + externalEventId
                + " groupEventId="
                + event.getId()
                + " payload="
                + body,
            () ->
                restClient
                    .put()
                    .uri(
                        uriBuilder ->
                            uriBuilder
                                .scheme("https")
                                .host("www.googleapis.com")
                                .pathSegment(
                                    "calendar",
                                    "v3",
                                    "calendars",
                                    calendarId,
                                    "events",
                                    externalEventId)
                                .build())
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .body(body)
                    .retrieve()
                    .body(GoogleCalendarWriteResponse.class));

    if (updated == null || updated.getId() == null) {
      throw new IllegalStateException("Google event update failed");
    }
    return updated;
  }

  private void deleteSyncs(List<GroupEventExternalSync> syncs) {
    for (GroupEventExternalSync sync : syncs) {
      String accessToken = tokenService.activeAccessToken(sync.getExternalCalendarAccount());
      try {
        deleteGoogleEvent(sync.getExternalCalendarId(), sync.getExternalEventId(), accessToken);
        sync.setStatus(GroupEventExternalSyncStatus.DELETED);
        sync.setLastError(null);
      } catch (RuntimeException ex) {
        sync.setStatus(GroupEventExternalSyncStatus.FAILED);
        sync.setLastError(truncateError(ex.getMessage()));
      }
      sync.setLastSyncedAt(OffsetDateTime.now());
      syncRepository.save(sync);
    }
  }

  private void deleteGoogleEvent(String calendarId, String externalEventId, String accessToken) {
    executeGoogleWrite(
        "delete Google event",
        () -> {
          restClient
              .delete()
              .uri(
                  uriBuilder ->
                      uriBuilder
                          .scheme("https")
                          .host("www.googleapis.com")
                          .pathSegment(
                              "calendar", "v3", "calendars", calendarId, "events", externalEventId)
                          .build())
              .headers(headers -> headers.setBearerAuth(accessToken))
              .retrieve()
              .toBodilessEntity();
          return null;
        });
  }

  private Map<String, Object> toGoogleEventBody(GroupEvent event) {
    return Map.of(
        "summary",
        event.getTitle(),
        "description",
        event.getDescription() == null ? "" : event.getDescription(),
        "start",
        Map.of("dateTime", formatGoogleDateTime(event.getStartsAt())),
        "end",
        Map.of("dateTime", formatGoogleDateTime(event.getEndsAt())));
  }

  private void upsertLocalGoogleCalendar(
      AppUser user, ExternalCalendarAccount account, String externalId, String name) {
    Calendar calendar =
        calendarRepository
            .findByUserAndProviderAndExternalId(user, CalendarProvider.GOOGLE, externalId)
            .orElseGet(
                () ->
                    Calendar.builder()
                        .user(user)
                        .provider(CalendarProvider.GOOGLE)
                        .externalId(externalId)
                        .isActive(true)
                        .build());

    calendar.setName(name == null ? MEETLER_CALENDAR_NAME : name);
    calendar.setEditable(true);
    calendar.setExternalCalendarAccount(account);
    calendar.setSyncDirection(CalendarSynchronizationType.TO_PROVIDER);
    calendarRepository.save(calendar);
  }

  private GoogleCalendarWriteResponse googleCalendar(String id, String summary, String color) {
    GoogleCalendarWriteResponse response = new GoogleCalendarWriteResponse();
    response.setId(id);
    response.setSummary(summary);
    response.setBackgroundColor(color);
    return response;
  }

  private String truncateError(String message) {
    if (message == null) {
      return null;
    }
    return message.length() <= 2000 ? message : message.substring(0, 2000);
  }

  private String formatGoogleDateTime(OffsetDateTime dateTime) {
    return dateTime.withOffsetSameInstant(ZoneOffset.UTC).format(GOOGLE_DATE_TIME_FORMATTER);
  }

  private <T> T executeGoogleWrite(String operation, GoogleWriteOperation<T> writeOperation) {
    try {
      return writeOperation.execute();
    } catch (RestClientResponseException ex) {
      log.warn(
          "Google Calendar {} failed with status {} and response body: {}",
          operation,
          ex.getStatusCode().value(),
          truncateError(ex.getResponseBodyAsString()));
      throw new ResponseStatusException(
          HttpStatus.BAD_GATEWAY,
          "Google Calendar "
              + operation
              + " failed with status "
              + ex.getStatusCode().value()
              + ": "
              + truncateError(ex.getResponseBodyAsString()),
          ex);
    }
  }

  @FunctionalInterface
  private interface GoogleWriteOperation<T> {
    T execute();
  }
}
