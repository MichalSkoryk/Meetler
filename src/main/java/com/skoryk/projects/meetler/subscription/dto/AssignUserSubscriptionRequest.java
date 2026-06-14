package com.skoryk.projects.meetler.subscription.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AssignUserSubscriptionRequest {

  @NotBlank private String planCode;
}
