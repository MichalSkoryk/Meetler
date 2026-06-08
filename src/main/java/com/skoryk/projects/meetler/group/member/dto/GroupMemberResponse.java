package com.skoryk.projects.meetler.group.member.dto;

import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GroupMemberResponse {
  private UUID id;
  private UUID userId;
  private String userName;
  private String userEmail;
  private String role;
  private OffsetDateTime joinedAt;
  private UUID availabilityTemplateId;
  private String availabilityTemplateName;
}
