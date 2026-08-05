package com.skoryk.projects.meetler.notification;

import com.skoryk.projects.meetler.notification.dto.NotificationResponse;
import java.time.OffsetDateTime;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface NotificationMapper {

  @Mapping(target = "read", source = "readAt", qualifiedByName = "isRead")
  NotificationResponse toResponse(Notification notification);

  @Named("isRead")
  default boolean isRead(OffsetDateTime readAt) {
    return readAt != null;
  }
}
