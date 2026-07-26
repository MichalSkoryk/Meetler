package com.skoryk.projects.meetler.calendar.external.google;

import com.skoryk.projects.meetler.calendar.CalendarProvider;
import com.skoryk.projects.meetler.calendar.external.ExternalCalendarAccountRepository;
import com.skoryk.projects.meetler.user.AppUser;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
    prefix = "external-calendar.google.sync",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
public class GoogleCalendarSyncScheduler {

  private final ExternalCalendarAccountRepository accountRepository;
  private final GoogleCalendarImportService importService;

  @Scheduled(
      fixedDelayString = "${external-calendar.google.sync.fixed-delay-ms:300000}",
      initialDelayString = "${external-calendar.google.sync.initial-delay-ms:30000}")
  void synchronize() {
    Map<UUID, AppUser> connectedUsers = new LinkedHashMap<>();
    accountRepository.findByProviderAndRevokedAtIsNull(CalendarProvider.GOOGLE).stream()
        .map(account -> account.getUser())
        .forEach(user -> connectedUsers.putIfAbsent(user.getId(), user));

    for (AppUser user : connectedUsers.values()) {
      try {
        importService.importCalendars(user, null, null);
      } catch (RuntimeException exception) {
        log.warn(
            "Automatic Google Calendar synchronization failed for user {}",
            user.getId(),
            exception);
      }
    }
  }
}
