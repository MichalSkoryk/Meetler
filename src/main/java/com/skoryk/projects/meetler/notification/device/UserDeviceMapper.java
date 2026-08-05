package com.skoryk.projects.meetler.notification.device;

import com.skoryk.projects.meetler.notification.dto.UserDeviceResponse;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface UserDeviceMapper {
  @BeanMapping(ignoreByDefault = true)
  @Mapping(target = "id", source = "device.id")
  @Mapping(target = "platform", source = "device.platform")
  @Mapping(target = "provider", source = "device.provider")
  @Mapping(target = "enabled", source = "device.enabled")
  @Mapping(target = "lastSeenAt", source = "device.lastSeenAt")
  UserDeviceResponse toResponse(UserDevice device);
}
