package com.skoryk.projects.meetler.me;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ConnectedCalendarsResponse {
  private boolean google;
  private boolean microsoft;
}
