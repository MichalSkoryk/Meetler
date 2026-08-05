package com.skoryk.projects.meetler.user;

import com.skoryk.projects.meetler.user.dto.UserResponse;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface AppUserMapper {

  @BeanMapping(ignoreByDefault = true)
  @Mapping(target = "id", source = "appUser.id")
  @Mapping(target = "email", source = "appUser.email")
  @Mapping(target = "name", source = "appUser.name")
  @Mapping(target = "role", source = "appUser.role")
  @Mapping(target = "createdAt", source = "appUser.createdAt")
  @Mapping(target = "upgradedAt", source = "appUser.upgradedAt")
  @Mapping(target = "deletedAt", source = "appUser.deletedAt")
  UserResponse toResponse(AppUser appUser);
}
