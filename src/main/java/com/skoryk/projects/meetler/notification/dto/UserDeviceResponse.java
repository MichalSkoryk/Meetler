package com.skoryk.projects.meetler.notification.dto;

import com.skoryk.projects.meetler.notification.device.UserDevicePlatform;
import com.skoryk.projects.meetler.notification.device.UserDeviceProvider;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserDeviceResponse {
  private UUID id;
  private UserDevicePlatform platform;
  private UserDeviceProvider provider;
  private boolean enabled;
  private OffsetDateTime lastSeenAt;
}
