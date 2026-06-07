package com.skoryk.projects.meetler.group.member.dto;

import java.util.UUID;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GroupAvailabilityTemplateResponse {

  private UUID groupId;
  private UUID userId;
  private UUID availabilityTemplateId;
  private String availabilityTemplateName;
  private String timezone;
}
