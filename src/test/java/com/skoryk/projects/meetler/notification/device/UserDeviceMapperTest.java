package com.skoryk.projects.meetler.notification.device;

import static org.assertj.core.api.Assertions.assertThat;

import com.skoryk.projects.meetler.notification.dto.UserDeviceResponse;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class UserDeviceMapperTest {

  private final UserDeviceMapper mapper = Mappers.getMapper(UserDeviceMapper.class);

  @Test
  void mapsOnlyPublicDeviceFields() {
    OffsetDateTime lastSeenAt = OffsetDateTime.parse("2026-08-05T12:00:00+02:00");
    UserDevice device =
        UserDevice.builder()
            .id(UUID.randomUUID())
            .platform(UserDevicePlatform.ANDROID)
            .provider(UserDeviceProvider.FCM)
            .token("secret-device-token")
            .enabled(true)
            .lastSeenAt(lastSeenAt)
            .build();

    UserDeviceResponse response = mapper.toResponse(device);

    assertThat(response.getId()).isEqualTo(device.getId());
    assertThat(response.getPlatform()).isEqualTo(UserDevicePlatform.ANDROID);
    assertThat(response.getProvider()).isEqualTo(UserDeviceProvider.FCM);
    assertThat(response.isEnabled()).isTrue();
    assertThat(response.getLastSeenAt()).isEqualTo(lastSeenAt);
    assertThat(
            Arrays.stream(UserDeviceResponse.class.getDeclaredFields())
                .map(field -> field.getName()))
        .doesNotContain("token", "user", "revokedAt");
  }
}
