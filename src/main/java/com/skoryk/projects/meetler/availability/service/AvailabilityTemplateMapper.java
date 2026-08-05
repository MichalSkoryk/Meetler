package com.skoryk.projects.meetler.availability.service;

import com.skoryk.projects.meetler.availability.dto.AvailabilityBlockResponse;
import com.skoryk.projects.meetler.availability.dto.AvailabilityTemplateResponse;
import com.skoryk.projects.meetler.availability.dto.RecurringAvailabilityBlockResponse;
import com.skoryk.projects.meetler.availability.dto.SourceCalendarResponse;
import com.skoryk.projects.meetler.availability.model.AvailabilityTemplate;
import com.skoryk.projects.meetler.availability.model.AvailabilityTemplateBlock;
import com.skoryk.projects.meetler.availability.model.AvailabilityTemplateRecurringBlock;
import com.skoryk.projects.meetler.availability.model.AvailabilityTemplateSourceCalendar;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface AvailabilityTemplateMapper {

  @Mapping(target = "isDefault", source = "default")
  AvailabilityTemplateResponse toTemplateResponse(AvailabilityTemplate template);

  AvailabilityBlockResponse toBlockResponse(AvailabilityTemplateBlock block);

  RecurringAvailabilityBlockResponse toRecurringBlockResponse(
      AvailabilityTemplateRecurringBlock block);

  @Mapping(target = "calendarId", source = "calendar.id")
  @Mapping(target = "calendarName", source = "calendar.name")
  @Mapping(target = "provider", source = "calendar.provider")
  SourceCalendarResponse toSourceCalendarResponse(AvailabilityTemplateSourceCalendar source);
}
