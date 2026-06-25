package com.skoryk.projects.meetler.notification.dto;

import com.skoryk.projects.meetler.notification.device.UserDevicePlatform;
import com.skoryk.projects.meetler.notification.device.UserDeviceProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterDeviceRequest {

  @NotNull private UserDevicePlatform platform;

  @NotNull private UserDeviceProvider provider;

  @NotBlank private String token;
}
