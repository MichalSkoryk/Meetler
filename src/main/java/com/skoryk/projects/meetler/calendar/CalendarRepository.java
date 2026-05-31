package com.skoryk.projects.meetler.calendar;

import com.skoryk.projects.meetler.user.AppUser;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CalendarRepository extends JpaRepository<Calendar, UUID> {

  List<Calendar> findByUser(AppUser user);
}
