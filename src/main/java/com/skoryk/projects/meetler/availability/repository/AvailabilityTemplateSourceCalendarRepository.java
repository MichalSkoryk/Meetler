package com.skoryk.projects.meetler.availability.repository;

import com.skoryk.projects.meetler.availability.model.AvailabilityTemplate;
import com.skoryk.projects.meetler.availability.model.AvailabilityTemplateSourceCalendar;
import com.skoryk.projects.meetler.calendar.Calendar;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AvailabilityTemplateSourceCalendarRepository
    extends JpaRepository<AvailabilityTemplateSourceCalendar, UUID> {

  List<AvailabilityTemplateSourceCalendar> findByTemplate(AvailabilityTemplate template);

  Optional<AvailabilityTemplateSourceCalendar> findByIdAndTemplate(
      UUID id, AvailabilityTemplate template);

  Optional<AvailabilityTemplateSourceCalendar> findByTemplateAndCalendar(
      AvailabilityTemplate template, Calendar calendar);
}


