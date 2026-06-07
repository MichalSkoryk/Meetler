package com.skoryk.projects.meetler.calendar.external.google;

import java.util.List;
import lombok.Data;

@Data
public class GoogleCalendarEventsResponse {

  private List<GoogleCalendarEventResponse> items;

  @Data
  public static class GoogleCalendarEventResponse {
    private String id;
    private String summary;
    private String transparency;
    private GoogleCalendarEventDate start;
    private GoogleCalendarEventDate end;
  }

  @Data
  public static class GoogleCalendarEventDate {
    private String date;
    private String dateTime;
    private String timeZone;
  }
}
