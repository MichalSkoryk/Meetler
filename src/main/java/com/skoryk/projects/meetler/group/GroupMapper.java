package com.skoryk.projects.meetler.group;

import com.skoryk.projects.meetler.availability.model.AvailabilityTemplate;
import com.skoryk.projects.meetler.group.dto.GroupEventSummaryResponse;
import com.skoryk.projects.meetler.group.dto.GroupResponse;
import com.skoryk.projects.meetler.group.event.GroupEvent;
import com.skoryk.projects.meetler.group.event.GroupEventParticipant;
import com.skoryk.projects.meetler.group.member.GroupMember;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface GroupMapper {

  @Mapping(target = "id", source = "membership.group.id")
  @Mapping(target = "name", source = "membership.group.name")
  @Mapping(target = "role", source = "membership.role")
  @Mapping(target = "memberCount", source = "memberCount")
  @Mapping(
      target = "hasAvailabilityTemplate",
      source = "membership.availabilityTemplate",
      qualifiedByName = "hasAvailabilityTemplate")
  @Mapping(target = "pendingResponseCount", source = "pendingResponseCount")
  @Mapping(target = "nextEvent", source = "nextEvent")
  @Mapping(target = "nextPendingEvent", source = "nextPendingEvent")
  @Mapping(
      target = "eventRequiresConfirmation",
      source = "membership.group.eventRequiresConfirmation")
  @Mapping(target = "createdAt", source = "membership.group.createdAt")
  @Mapping(target = "updatedAt", source = "membership.group.updatedAt")
  GroupResponse toResponse(
      GroupMember membership,
      long memberCount,
      long pendingResponseCount,
      GroupEventSummaryResponse nextEvent,
      GroupEventSummaryResponse nextPendingEvent);

  @Mapping(target = "id", source = "event.id")
  @Mapping(target = "title", source = "event.title")
  @Mapping(target = "startsAt", source = "event.startsAt")
  @Mapping(target = "endsAt", source = "event.endsAt")
  @Mapping(target = "status", source = "event.status")
  @Mapping(target = "requiresConfirmation", source = "event.requiresConfirmation")
  @Mapping(target = "myResponseStatus", source = "participant.status")
  GroupEventSummaryResponse toEventSummary(GroupEvent event, GroupEventParticipant participant);

  @Named("hasAvailabilityTemplate")
  default boolean hasAvailabilityTemplate(AvailabilityTemplate template) {
    return template != null;
  }
}
