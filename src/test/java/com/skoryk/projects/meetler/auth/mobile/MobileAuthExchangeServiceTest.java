package com.skoryk.projects.meetler.auth.mobile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.skoryk.projects.meetler.auth.AuthService;
import com.skoryk.projects.meetler.auth.dto.AuthResponse;
import com.skoryk.projects.meetler.user.AppUser;
import com.skoryk.projects.meetler.user.AppUserRole;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class MobileAuthExchangeServiceTest {

  @Mock private MobileAuthExchangeCodeRepository repository;
  @Mock private AuthService authService;
  @InjectMocks private MobileAuthExchangeService service;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(service, "ttlSeconds", 120L);
  }

  @Test
  void createsOnlyAHashAndExpiresAfterTwoMinutes() {
    AppUser user = user();
    when(repository.save(any(MobileAuthExchangeCode.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    String rawCode = service.createCode(user);

    ArgumentCaptor<MobileAuthExchangeCode> captor =
        ArgumentCaptor.forClass(MobileAuthExchangeCode.class);
    verify(repository).save(captor.capture());
    assertThat(rawCode).isNotBlank();
    assertThat(captor.getValue().getCodeHash()).hasSize(64).isNotEqualTo(rawCode);
    assertThat(captor.getValue().getExpiresAt())
        .isBetween(OffsetDateTime.now().plusSeconds(115), OffsetDateTime.now().plusSeconds(125));
  }

  @Test
  void exchangeConsumesCodeAndIssuesTokensOnce() {
    AppUser user = user();
    MobileAuthExchangeCode code =
        MobileAuthExchangeCode.builder()
            .user(user)
            .expiresAt(OffsetDateTime.now().plusMinutes(1))
            .build();
    AuthResponse tokens = new AuthResponse("access", "refresh");
    when(repository.findForUpdateByCodeHash(anyString())).thenReturn(Optional.of(code));
    when(authService.issueTokens(user)).thenReturn(tokens);

    assertThat(service.exchange("single-use-code")).isEqualTo(tokens);
    assertThat(code.getUsedAt()).isNotNull();
    verify(repository).save(code);

    assertThatThrownBy(() -> service.exchange("single-use-code"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Mobile exchange code expired or already used");
  }

  @Test
  void expiredCodeCannotIssueTokens() {
    MobileAuthExchangeCode code =
        MobileAuthExchangeCode.builder()
            .user(user())
            .expiresAt(OffsetDateTime.now().minusSeconds(1))
            .build();
    when(repository.findForUpdateByCodeHash(anyString())).thenReturn(Optional.of(code));

    assertThatThrownBy(() -> service.exchange("expired-code"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Mobile exchange code expired or already used");
    verify(authService, never()).issueTokens(any());
  }

  private AppUser user() {
    return AppUser.builder()
        .id(UUID.randomUUID())
        .email("mobile@example.com")
        .role(AppUserRole.USER)
        .build();
  }
}
