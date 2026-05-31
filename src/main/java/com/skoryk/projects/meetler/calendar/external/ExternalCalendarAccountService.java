package com.skoryk.projects.meetler.calendar.external;

import com.skoryk.projects.meetler.calendar.CalendarProvider;
import com.skoryk.projects.meetler.calendar.external.dto.ExternalCalendarAccountResponse;
import com.skoryk.projects.meetler.calendar.external.dto.ExternalCalendarTokens;
import com.skoryk.projects.meetler.calendar.external.dto.StoreExternalCalendarAccountRequest;
import com.skoryk.projects.meetler.user.AppUser;
import jakarta.validation.Valid;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
public class ExternalCalendarAccountService {

  private final ExternalCalendarAccountRepository accountRepository;
  private final TokenEncryptionService tokenEncryptionService;

  @Transactional
  public ExternalCalendarAccountResponse storeOrUpdateAccount(
      AppUser user, @Valid StoreExternalCalendarAccountRequest request) {
    if (request.getProvider() == CalendarProvider.INTERNAL) {
      throw new IllegalArgumentException("Internal calendars do not use external account tokens");
    }

    ExternalCalendarAccount account =
        accountRepository
            .findByUserAndProviderAndExternalAccountId(
                user, request.getProvider(), request.getExternalAccountId())
            .orElseGet(
                () ->
                    ExternalCalendarAccount.builder()
                        .user(user)
                        .provider(request.getProvider())
                        .externalAccountId(request.getExternalAccountId())
                        .build());

    account.setAccountEmail(request.getAccountEmail());
    account.setScopes(request.getScopes());
    account.setAccessTokenEncrypted(tokenEncryptionService.encrypt(request.getAccessToken()));
    account.setTokenType(request.getTokenType());
    account.setExpiresAt(request.getExpiresAt());
    account.setRevokedAt(null);

    if (request.getRefreshToken() != null && !request.getRefreshToken().isBlank()) {
      account.setRefreshTokenEncrypted(tokenEncryptionService.encrypt(request.getRefreshToken()));
    }

    return ExternalCalendarAccountResponse.from(accountRepository.save(account));
  }

  public List<ExternalCalendarAccountResponse> listActiveAccounts(AppUser user) {
    return accountRepository.findByUserAndRevokedAtIsNull(user).stream()
        .map(ExternalCalendarAccountResponse::from)
        .toList();
  }

  public ExternalCalendarTokens getDecryptedTokens(UUID accountId, AppUser user) {
    ExternalCalendarAccount account =
        accountRepository
            .findByIdAndUser(accountId, user)
            .orElseThrow(() -> new IllegalArgumentException("External calendar account not found"));

    if (account.getRevokedAt() != null) {
      throw new IllegalArgumentException("External calendar account is revoked");
    }

    return ExternalCalendarTokens.builder()
        .accessToken(tokenEncryptionService.decrypt(account.getAccessTokenEncrypted()))
        .refreshToken(tokenEncryptionService.decrypt(account.getRefreshTokenEncrypted()))
        .tokenType(account.getTokenType())
        .expiresAt(account.getExpiresAt())
        .build();
  }

  @Transactional
  public void markSynced(UUID accountId, AppUser user) {
    ExternalCalendarAccount account =
        accountRepository
            .findByIdAndUser(accountId, user)
            .orElseThrow(() -> new IllegalArgumentException("External calendar account not found"));

    account.setLastSyncedAt(OffsetDateTime.now());
    accountRepository.save(account);
  }

  @Transactional
  public void revoke(UUID accountId, AppUser user) {
    ExternalCalendarAccount account =
        accountRepository
            .findByIdAndUser(accountId, user)
            .orElseThrow(() -> new IllegalArgumentException("External calendar account not found"));

    account.setRevokedAt(OffsetDateTime.now());
    accountRepository.save(account);
  }
}
