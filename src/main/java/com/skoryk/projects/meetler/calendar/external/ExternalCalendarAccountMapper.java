package com.skoryk.projects.meetler.calendar.external;

import com.skoryk.projects.meetler.calendar.external.dto.ExternalCalendarAccountResponse;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ExternalCalendarAccountMapper {

  @BeanMapping(ignoreByDefault = true)
  @Mapping(target = "id", source = "id")
  @Mapping(target = "provider", source = "provider")
  @Mapping(target = "externalAccountId", source = "externalAccountId")
  @Mapping(target = "accountEmail", source = "accountEmail")
  @Mapping(target = "scopes", source = "scopes")
  @Mapping(target = "tokenType", source = "tokenType")
  @Mapping(target = "expiresAt", source = "expiresAt")
  @Mapping(target = "lastSyncedAt", source = "lastSyncedAt")
  @Mapping(target = "revokedAt", source = "revokedAt")
  @Mapping(target = "createdAt", source = "createdAt")
  @Mapping(target = "updatedAt", source = "updatedAt")
  ExternalCalendarAccountResponse toResponse(ExternalCalendarAccount externalCalendarAccount);
}
