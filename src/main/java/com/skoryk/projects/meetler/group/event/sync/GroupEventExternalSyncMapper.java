package com.skoryk.projects.meetler.group.event.sync;

import com.skoryk.projects.meetler.group.event.dto.ExportGroupEventResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface GroupEventExternalSyncMapper {

  @Mapping(target = "groupEventId", source = "groupEvent.id")
  @Mapping(target = "externalCalendarAccountId", source = "externalCalendarAccount.id")
  ExportGroupEventResponse toResponse(GroupEventExternalSync sync);
}
