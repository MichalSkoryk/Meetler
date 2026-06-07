package com.skoryk.projects.meetler.availability.repository;

import com.skoryk.projects.meetler.availability.model.AvailabilityTemplate;
import com.skoryk.projects.meetler.availability.model.AvailabilityTemplateRecurringBlock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AvailabilityTemplateRecurringBlockRepository
    extends JpaRepository<AvailabilityTemplateRecurringBlock, UUID> {

  @Query(
      """
      select b
      from AvailabilityTemplateRecurringBlock b
      where b.template = :template
        and (:from is null or b.endsOn is null or b.endsOn >= :from)
        and (:to is null or b.startsOn is null or b.startsOn <= :to)
      order by b.frequency asc, b.dayOfWeek asc, b.monthOfYear asc, b.dayOfMonth asc, b.startTime asc
      """)
  Page<AvailabilityTemplateRecurringBlock> findByTemplateActiveInDateRange(
      AvailabilityTemplate template, LocalDate from, LocalDate to, Pageable pageable);

  @Query(
      """
      select b
      from AvailabilityTemplateRecurringBlock b
      where b.template = :template
        and (b.endsOn is null or b.endsOn >= :from)
        and (b.startsOn is null or b.startsOn <= :to)
      order by b.frequency asc, b.dayOfWeek asc, b.monthOfYear asc, b.dayOfMonth asc, b.startTime asc
      """)
  List<AvailabilityTemplateRecurringBlock> findByTemplateActiveInDateRange(
      AvailabilityTemplate template, LocalDate from, LocalDate to);

  Optional<AvailabilityTemplateRecurringBlock> findByIdAndTemplate(
      UUID id, AvailabilityTemplate template);
}


