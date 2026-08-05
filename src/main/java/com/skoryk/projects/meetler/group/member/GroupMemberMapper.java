package com.skoryk.projects.meetler.group.member;

import com.skoryk.projects.meetler.group.member.dto.GroupAvailabilityTemplateResponse;
import com.skoryk.projects.meetler.group.member.dto.GroupMemberResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface GroupMemberMapper {

  @Mapping(target = "userId", source = "user.id")
  @Mapping(target = "userName", source = "user.name")
  @Mapping(target = "userEmail", source = "user.email")
  @Mapping(target = "role", source = "role")
  @Mapping(target = "availabilityTemplateId", source = "availabilityTemplate.id")
  @Mapping(target = "availabilityTemplateName", source = "availabilityTemplate.name")
  GroupMemberResponse toMemberResponse(GroupMember member);

  @Mapping(target = "groupId", source = "group.id")
  @Mapping(target = "userId", source = "user.id")
  @Mapping(target = "availabilityTemplateId", source = "availabilityTemplate.id")
  @Mapping(target = "availabilityTemplateName", source = "availabilityTemplate.name")
  @Mapping(target = "timezone", source = "availabilityTemplate.timezone")
  GroupAvailabilityTemplateResponse toAvailabilityTemplateResponse(GroupMember member);
}
