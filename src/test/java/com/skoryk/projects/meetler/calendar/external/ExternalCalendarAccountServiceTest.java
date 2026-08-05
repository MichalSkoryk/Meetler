package com.skoryk.projects.meetler.calendar.external;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.skoryk.projects.meetler.calendar.CalendarProvider;
import com.skoryk.projects.meetler.calendar.external.dto.ExternalCalendarTokens;
import com.skoryk.projects.meetler.calendar.external.dto.StoreExternalCalendarAccountRequest;
import com.skoryk.projects.meetler.user.AppUser;
import com.skoryk.projects.meetler.user.AppUserRole;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExternalCalendarAccountServiceTest {

  @Mock private ExternalCalendarAccountRepository accountRepository;
  @Mock private TokenEncryptionService tokenEncryptionService;

  @Spy
  private ExternalCalendarAccountMapper externalCalendarAccountMapper =
      Mappers.getMapper(ExternalCalendarAccountMapper.class);

  @InjectMocks private ExternalCalendarAccountService service;

  @Test
  void storeOrUpdateRejectsInternalProvider() {
    StoreExternalCalendarAccountRequest request = request();
    request.setProvider(CalendarProvider.INTERNAL);

    assertThatThrownBy(() -> service.storeOrUpdateAccount(user(), request))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Internal calendars do not use external account tokens");

    verify(accountRepository, never()).save(any());
  }

  @Test
  void storeOrUpdatePreservesRefreshTokenWhenProviderDoesNotReturnOne() {
    AppUser user = user();
    StoreExternalCalendarAccountRequest request = request();
    request.setRefreshToken("");
    ExternalCalendarAccount existing =
        ExternalCalendarAccount.builder()
            .id(UUID.randomUUID())
            .user(user)
            .provider(CalendarProvider.GOOGLE)
            .externalAccountId("google-id")
            .refreshTokenEncrypted("old-refresh")
            .build();
    when(accountRepository.findByUserAndProviderAndExternalAccountId(
            user, CalendarProvider.GOOGLE, "google-id"))
        .thenReturn(Optional.of(existing));
    when(tokenEncryptionService.encrypt("access")).thenReturn("encrypted-access");
    when(accountRepository.save(existing)).thenReturn(existing);

    service.storeOrUpdateAccount(user, request);

    assertThat(existing.getAccessTokenEncrypted()).isEqualTo("encrypted-access");
    assertThat(existing.getRefreshTokenEncrypted()).isEqualTo("old-refresh");
  }

  @Test
  void getDecryptedTokensRejectsRevokedAccount() {
    AppUser user = user();
    ExternalCalendarAccount account = account(user);
    account.setRevokedAt(OffsetDateTime.now());
    when(accountRepository.findByIdAndUser(account.getId(), user)).thenReturn(Optional.of(account));

    assertThatThrownBy(() -> service.getDecryptedTokens(account.getId(), user))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("External calendar account is revoked");
  }

  @Test
  void getDecryptedTokensDecryptsStoredTokens() {
    AppUser user = user();
    ExternalCalendarAccount account = account(user);
    account.setAccessTokenEncrypted("encrypted-access");
    account.setRefreshTokenEncrypted("encrypted-refresh");
    account.setTokenType("Bearer");
    when(accountRepository.findByIdAndUser(account.getId(), user)).thenReturn(Optional.of(account));
    when(tokenEncryptionService.decrypt("encrypted-access")).thenReturn("access");
    when(tokenEncryptionService.decrypt("encrypted-refresh")).thenReturn("refresh");

    ExternalCalendarTokens tokens = service.getDecryptedTokens(account.getId(), user);

    assertThat(tokens.getAccessToken()).isEqualTo("access");
    assertThat(tokens.getRefreshToken()).isEqualTo("refresh");
    assertThat(tokens.getTokenType()).isEqualTo("Bearer");
  }

  private StoreExternalCalendarAccountRequest request() {
    StoreExternalCalendarAccountRequest request = new StoreExternalCalendarAccountRequest();
    request.setProvider(CalendarProvider.GOOGLE);
    request.setExternalAccountId("google-id");
    request.setAccountEmail("google@example.com");
    request.setAccessToken("access");
    request.setRefreshToken("refresh");
    request.setTokenType("Bearer");
    return request;
  }

  private ExternalCalendarAccount account(AppUser user) {
    return ExternalCalendarAccount.builder()
        .id(UUID.randomUUID())
        .user(user)
        .provider(CalendarProvider.GOOGLE)
        .externalAccountId("google-id")
        .build();
  }

  private AppUser user() {
    return AppUser.builder()
        .id(UUID.randomUUID())
        .email("test@example.com")
        .role(AppUserRole.USER)
        .build();
  }
}
