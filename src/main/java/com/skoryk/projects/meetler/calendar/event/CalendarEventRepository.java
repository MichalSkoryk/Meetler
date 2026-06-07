package com.skoryk.projects.meetler.calendar.event;

import com.skoryk.projects.meetler.calendar.Calendar;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CalendarEventRepository extends JpaRepository<CalendarEvent, UUID> {

  Optional<CalendarEvent> findByCalendarAndExternalId(Calendar calendar, String externalId);

  @Query(
      """
      select e
      from CalendarEvent e
      where e.calendar in :calendars
        and e.busy = true
        and e.endsAt > :from
        and e.startsAt < :to
      order by e.startsAt asc
      """)
  List<CalendarEvent> findBusyEventsOverlapping(
      Collection<Calendar> calendars, OffsetDateTime from, OffsetDateTime to);
}
