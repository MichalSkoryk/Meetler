package com.skoryk.projects.meetler.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MobileAuthExchangeRequest {

  @NotBlank private String code;
}
