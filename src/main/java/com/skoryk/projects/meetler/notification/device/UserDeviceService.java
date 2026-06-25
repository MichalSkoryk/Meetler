package com.skoryk.projects.meetler.notification.device;

import com.skoryk.projects.meetler.notification.dto.RegisterDeviceRequest;
import com.skoryk.projects.meetler.notification.dto.UserDeviceResponse;
import com.skoryk.projects.meetler.user.AppUser;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserDeviceService {

  private final UserDeviceRepository userDeviceRepository;

  @Transactional
  public UserDeviceResponse registerDevice(AppUser user, RegisterDeviceRequest request) {
    OffsetDateTime now = OffsetDateTime.now();
    UserDevice device =
        userDeviceRepository
            .findByUserAndToken(user, request.getToken())
            .orElseGet(
                () ->
                    UserDevice.builder()
                        .user(user)
                        .token(request.getToken())
                        .enabled(true)
                        .build());

    device.setPlatform(request.getPlatform());
    device.setProvider(request.getProvider());
    device.setEnabled(true);
    device.setRevokedAt(null);
    device.setLastSeenAt(now);

    return toResponse(userDeviceRepository.save(device));
  }

  @Transactional
  public void revokeDevice(AppUser user, UUID deviceId) {
    UserDevice device =
        userDeviceRepository
            .findById(deviceId)
            .orElseThrow(() -> new IllegalArgumentException("Device not found"));
    if (!device.getUser().getId().equals(user.getId())) {
      throw new IllegalArgumentException("Device not found");
    }

    device.setEnabled(false);
    device.setRevokedAt(OffsetDateTime.now());
    userDeviceRepository.save(device);
  }

  private UserDeviceResponse toResponse(UserDevice device) {
    return UserDeviceResponse.builder()
        .id(device.getId())
        .platform(device.getPlatform())
        .provider(device.getProvider())
        .enabled(device.isEnabled())
        .lastSeenAt(device.getLastSeenAt())
        .build();
  }
}
