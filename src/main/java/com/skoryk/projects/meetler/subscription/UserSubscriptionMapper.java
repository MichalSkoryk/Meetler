package com.skoryk.projects.meetler.subscription;

import com.skoryk.projects.meetler.subscription.dto.UserSubscriptionResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface UserSubscriptionMapper {

  @Mapping(target = "userId", source = "user.id")
  @Mapping(target = "planId", source = "plan.id")
  @Mapping(target = "planCode", source = "plan.code")
  @Mapping(target = "planName", source = "plan.name")
  UserSubscriptionResponse toResponse(UserSubscription subscription);
}
