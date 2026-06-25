package com.skoryk.projects.meetler.notification.device;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.skoryk.projects.meetler.notification.dto.RegisterDeviceRequest;
import com.skoryk.projects.meetler.notification.dto.UserDeviceResponse;
import com.skoryk.projects.meetler.user.AppUser;
import com.skoryk.projects.meetler.user.AppUserRole;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserDeviceServiceTest {

  @Mock private UserDeviceRepository userDeviceRepository;

  @InjectMocks private UserDeviceService userDeviceService;

  @Test
  void registerDeviceCreatesNewDeviceForToken() {
    AppUser user = user();
    RegisterDeviceRequest request = request("token");

    when(userDeviceRepository.findByUserAndToken(user, "token")).thenReturn(Optional.empty());
    when(userDeviceRepository.save(any(UserDevice.class)))
        .thenAnswer(
            invocation -> {
              UserDevice device = invocation.getArgument(0);
              device.setId(UUID.randomUUID());
              return device;
            });

    UserDeviceResponse response = userDeviceService.registerDevice(user, request);

    assertThat(response.getId()).isNotNull();
    assertThat(response.getPlatform()).isEqualTo(UserDevicePlatform.ANDROID);
    assertThat(response.getProvider()).isEqualTo(UserDeviceProvider.FCM);
    assertThat(response.isEnabled()).isTrue();
    assertThat(response.getLastSeenAt()).isNotNull();
  }

  @Test
  void revokeDeviceRejectsDeviceOwnedByAnotherUser() {
    AppUser user = user();
    AppUser otherUser = user();
    UserDevice device =
        UserDevice.builder().id(UUID.randomUUID()).user(otherUser).token("token").build();

    when(userDeviceRepository.findById(device.getId())).thenReturn(Optional.of(device));

    assertThatThrownBy(() -> userDeviceService.revokeDevice(user, device.getId()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Device not found");
  }

  @Test
  void revokeDeviceDisablesOwnedDevice() {
    AppUser user = user();
    UserDevice device =
        UserDevice.builder().id(UUID.randomUUID()).user(user).token("token").enabled(true).build();

    when(userDeviceRepository.findById(device.getId())).thenReturn(Optional.of(device));

    userDeviceService.revokeDevice(user, device.getId());

    assertThat(device.isEnabled()).isFalse();
    assertThat(device.getRevokedAt()).isNotNull();
    verify(userDeviceRepository).save(device);
  }

  private RegisterDeviceRequest request(String token) {
    RegisterDeviceRequest request = new RegisterDeviceRequest();
    request.setPlatform(UserDevicePlatform.ANDROID);
    request.setProvider(UserDeviceProvider.FCM);
    request.setToken(token);
    return request;
  }

  private AppUser user() {
    return AppUser.builder()
        .id(UUID.randomUUID())
        .email(UUID.randomUUID() + "@example.com")
        .role(AppUserRole.USER)
        .build();
  }
}
