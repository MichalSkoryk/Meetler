package com.skoryk.projects.meetler.calendar.external.google;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Data;

@Data
public class GoogleCalendarListResponse {

  private List<GoogleCalendarResponse> items;

  @Data
  public static class GoogleCalendarResponse {
    private String id;
    private String summary;

    @JsonProperty("backgroundColor")
    private String backgroundColor;

    private Boolean selected;
  }
}
