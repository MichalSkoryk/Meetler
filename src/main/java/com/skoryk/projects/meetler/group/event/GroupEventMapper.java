package com.skoryk.projects.meetler.group.event;

import com.skoryk.projects.meetler.group.event.dto.GroupEventParticipantResponse;
import com.skoryk.projects.meetler.group.event.dto.GroupEventResponse;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface GroupEventMapper {

  @Mapping(target = "id", source = "event.id")
  @Mapping(target = "groupId", source = "event.group.id")
  @Mapping(target = "createdByUserId", source = "event.createdBy.id")
  @Mapping(target = "title", source = "event.title")
  @Mapping(target = "description", source = "event.description")
  @Mapping(target = "startsAt", source = "event.startsAt")
  @Mapping(target = "endsAt", source = "event.endsAt")
  @Mapping(target = "status", source = "event.status")
  @Mapping(target = "requiresConfirmation", source = "event.requiresConfirmation")
  @Mapping(target = "participants", source = "participants")
  @Mapping(target = "createdAt", source = "event.createdAt")
  @Mapping(target = "updatedAt", source = "event.updatedAt")
  GroupEventResponse toResponse(GroupEvent event, List<GroupEventParticipant> participants);

  @Mapping(target = "userId", source = "user.id")
  @Mapping(target = "userEmail", source = "user.email")
  GroupEventParticipantResponse toParticipantResponse(GroupEventParticipant participant);
}
