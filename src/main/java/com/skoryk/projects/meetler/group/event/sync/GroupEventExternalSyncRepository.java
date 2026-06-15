package com.skoryk.projects.meetler.group.event.sync;

import com.skoryk.projects.meetler.calendar.CalendarProvider;
import com.skoryk.projects.meetler.calendar.external.ExternalCalendarAccount;
import com.skoryk.projects.meetler.group.event.GroupEvent;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupEventExternalSyncRepository
    extends JpaRepository<GroupEventExternalSync, UUID> {

  Optional<GroupEventExternalSync> findByGroupEventAndExternalCalendarAccountAndProvider(
      GroupEvent groupEvent,
      ExternalCalendarAccount externalCalendarAccount,
      CalendarProvider provider);

  List<GroupEventExternalSync> findByGroupEventAndProviderAndStatus(
      GroupEvent groupEvent, CalendarProvider provider, GroupEventExternalSyncStatus status);
}
