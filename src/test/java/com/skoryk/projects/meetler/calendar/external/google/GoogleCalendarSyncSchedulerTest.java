package com.skoryk.projects.meetler.calendar.external.google;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.skoryk.projects.meetler.calendar.CalendarProvider;
import com.skoryk.projects.meetler.calendar.external.ExternalCalendarAccount;
import com.skoryk.projects.meetler.calendar.external.ExternalCalendarAccountRepository;
import com.skoryk.projects.meetler.user.AppUser;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GoogleCalendarSyncSchedulerTest {

  @Mock private ExternalCalendarAccountRepository accountRepository;
  @Mock private GoogleCalendarImportService importService;

  @InjectMocks private GoogleCalendarSyncScheduler scheduler;

  @Test
  void synchronizesEachConnectedUserOnlyOnce() {
    AppUser firstUser = user();
    AppUser secondUser = user();
    when(accountRepository.findByProviderAndRevokedAtIsNull(CalendarProvider.GOOGLE))
        .thenReturn(List.of(account(firstUser), account(firstUser), account(secondUser)));

    scheduler.synchronize();

    verify(importService, times(1)).importCalendars(firstUser, null, null);
    verify(importService, times(1)).importCalendars(secondUser, null, null);
  }

  @Test
  void continuesWithOtherUsersWhenOneSynchronizationFails() {
    AppUser firstUser = user();
    AppUser secondUser = user();
    when(accountRepository.findByProviderAndRevokedAtIsNull(CalendarProvider.GOOGLE))
        .thenReturn(List.of(account(firstUser), account(secondUser)));
    doThrow(new IllegalStateException("expired token"))
        .when(importService)
        .importCalendars(firstUser, null, null);

    scheduler.synchronize();

    verify(importService).importCalendars(secondUser, null, null);
  }

  private ExternalCalendarAccount account(AppUser user) {
    return ExternalCalendarAccount.builder()
        .id(UUID.randomUUID())
        .user(user)
        .provider(CalendarProvider.GOOGLE)
        .build();
  }

  private AppUser user() {
    return AppUser.builder().id(UUID.randomUUID()).build();
  }
}
