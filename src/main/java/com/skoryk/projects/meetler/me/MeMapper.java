package com.skoryk.projects.meetler.me;

import com.skoryk.projects.meetler.subscription.SubscriptionUsageResponse;
import com.skoryk.projects.meetler.user.AppUser;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface MeMapper {

  @Mapping(target = "hasPassword", source = "passwordHash", qualifiedByName = "hasPassword")
  MeUserResponse toUserResponse(AppUser user);

  MeBootstrapResponse toBootstrapResponse(
      MeUserResponse user,
      SubscriptionUsageResponse subscription,
      ConnectedCalendarsResponse connectedCalendars,
      ConnectedLoginMethodsResponse connectedLoginMethods);

  ConnectedCalendarsResponse toConnectedCalendarsResponse(boolean google, boolean microsoft);

  ConnectedLoginMethodsResponse toConnectedLoginMethodsResponse(
      boolean password, boolean google, boolean microsoft);

  @Named("hasPassword")
  default boolean hasPassword(String passwordHash) {
    return passwordHash != null && !passwordHash.isBlank();
  }
}
