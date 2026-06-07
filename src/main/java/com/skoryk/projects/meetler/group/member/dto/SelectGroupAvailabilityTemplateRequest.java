package com.skoryk.projects.meetler.group.member.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Data;

@Data
public class SelectGroupAvailabilityTemplateRequest {

  @NotNull private UUID availabilityTemplateId;
}
