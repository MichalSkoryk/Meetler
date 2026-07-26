package com.skoryk.projects.meetler.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.skoryk.projects.meetler.auth.identity.UserAuthIdentity;
import com.skoryk.projects.meetler.auth.identity.UserAuthIdentityRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AppUserServiceTest {

  @Mock private AppUserRepository appUserRepository;
  @Mock private UserAuthIdentityRepository identityRepository;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private ApplicationEventPublisher eventPublisher;

  @InjectMocks private AppUserService service;

  @Test
  void createUserNormalizesEmailAndCreatesGuest() {
    when(appUserRepository.save(any(AppUser.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    AppUser user = service.createUser("TEST@EXAMPLE.COM");

    assertThat(user.getEmail()).isEqualTo("test@example.com");
    assertThat(user.getRole()).isEqualTo(AppUserRole.GUEST);
  }

  @Test
  void upgradeToUserReturnsExistingUserWithoutSavingAgain() {
    AppUser user = user();
    user.setRole(AppUserRole.USER);
    when(appUserRepository.findById(user.getId())).thenReturn(Optional.of(user));

    AppUser upgraded = service.upgradeToUser(user.getId());

    assertThat(upgraded).isSameAs(user);
    verify(appUserRepository, never()).save(any());
  }

  @Test
  void changePasswordRejectsWrongCurrentPassword() {
    AppUser user = user();
    user.setPasswordHash("old-hash");
    when(appUserRepository.findById(user.getId())).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("bad", "old-hash")).thenReturn(false);

    assertThatThrownBy(() -> service.changePassword(user, "bad", "new-secret"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Invalid current password");

    verify(appUserRepository, never()).save(any());
  }

  @Test
  void changePasswordUpdatesHashAndCreatesInternalIdentity() {
    AppUser user = user();
    user.setPasswordHash("old-hash");
    when(appUserRepository.findById(user.getId())).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("old", "old-hash")).thenReturn(true);
    when(passwordEncoder.matches("new", "old-hash")).thenReturn(false);
    when(passwordEncoder.encode("new")).thenReturn("new-hash");
    when(identityRepository.findByUserAndProviderAndProviderUserId(
            user, AuthProvider.INTERNAL, user.getEmail()))
        .thenReturn(Optional.empty());

    service.changePassword(user, "old", "new");

    assertThat(user.getPasswordHash()).isEqualTo("new-hash");
    verify(identityRepository).save(any(UserAuthIdentity.class));
    verify(appUserRepository).save(user);
  }

  @Test
  void setInitialPasswordRejectsUserThatAlreadyHasPassword() {
    AppUser user = user();
    user.setPasswordHash("existing");
    when(appUserRepository.findById(user.getId())).thenReturn(Optional.of(user));

    assertThatThrownBy(() -> service.setInitialPassword(user, "new"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("This user already has password");
  }

  @Test
  void setInitialPasswordConvertsGuestToUser() {
    AppUser guest = user();
    guest.setRole(AppUserRole.GUEST);
    when(appUserRepository.findById(guest.getId())).thenReturn(Optional.of(guest));
    when(passwordEncoder.encode("new-password")).thenReturn("new-hash");
    when(identityRepository.findByUserAndProviderAndProviderUserId(
            guest, AuthProvider.INTERNAL, guest.getEmail()))
        .thenReturn(Optional.empty());
    when(appUserRepository.save(guest)).thenReturn(guest);

    service.setInitialPassword(guest, "new-password");

    assertThat(guest.getRole()).isEqualTo(AppUserRole.USER);
    assertThat(guest.getUpgradedAt()).isNotNull();
    verify(eventPublisher).publishEvent(any(AccountConvertedEvent.class));
  }

  private AppUser user() {
    return AppUser.builder()
        .id(UUID.randomUUID())
        .email("test@example.com")
        .role(AppUserRole.USER)
        .build();
  }
}
