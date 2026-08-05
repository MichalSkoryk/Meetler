package com.skoryk.projects.meetler.group.invite;

import com.skoryk.projects.meetler.group.invite.dto.GroupInviteResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface GroupInviteMapper {
  @Mapping(target = "revokedByUserId", source = "revokedBy.id")
  GroupInviteResponse toResponse(GroupInvite invite);
}
