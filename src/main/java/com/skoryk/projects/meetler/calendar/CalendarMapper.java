package com.skoryk.projects.meetler.calendar;

import com.skoryk.projects.meetler.calendar.dto.CalendarResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface CalendarMapper {
  @Mapping(target = "isEditable", source = "calendar.editable")
  @Mapping(target = "isActive", source = "calendar.active")
  CalendarResponse toResponse(Calendar calendar);
}
