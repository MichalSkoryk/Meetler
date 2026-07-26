package com.skoryk.projects.meetler.calendar.external;

import com.skoryk.projects.meetler.calendar.CalendarProvider;
import com.skoryk.projects.meetler.user.AppUser;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExternalCalendarAccountRepository
    extends JpaRepository<ExternalCalendarAccount, UUID> {

  List<ExternalCalendarAccount> findByUserAndRevokedAtIsNull(AppUser user);

  List<ExternalCalendarAccount> findByUserAndProviderAndRevokedAtIsNull(
      AppUser user, CalendarProvider provider);

  @EntityGraph(attributePaths = "user")
  List<ExternalCalendarAccount> findByProviderAndRevokedAtIsNull(CalendarProvider provider);

  Optional<ExternalCalendarAccount> findByUserAndProviderAndExternalAccountId(
      AppUser user, CalendarProvider provider, String externalAccountId);

  Optional<ExternalCalendarAccount> findByIdAndUser(UUID id, AppUser user);
}
