package com.skoryk.projects.meetler.group.event.dto;

import com.skoryk.projects.meetler.group.event.GroupEventParticipantStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RespondToGroupEventRequest {

  @NotNull private GroupEventParticipantStatus status;
}
