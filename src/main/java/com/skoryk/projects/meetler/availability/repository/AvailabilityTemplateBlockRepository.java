package com.skoryk.projects.meetler.availability.repository;

import com.skoryk.projects.meetler.availability.model.AvailabilityTemplate;
import com.skoryk.projects.meetler.availability.model.AvailabilityTemplateBlock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AvailabilityTemplateBlockRepository
    extends JpaRepository<AvailabilityTemplateBlock, UUID> {

  Page<AvailabilityTemplateBlock> findByTemplateOrderByStartsAtAsc(
      AvailabilityTemplate template, Pageable pageable);

  @Query(
      """
      select b
      from AvailabilityTemplateBlock b
      where b.template = :template
        and b.endsAt > :from
      order by b.startsAt asc
      """)
  Page<AvailabilityTemplateBlock> findByTemplateEndingAfter(
      AvailabilityTemplate template, OffsetDateTime from, Pageable pageable);

  @Query(
      """
      select b
      from AvailabilityTemplateBlock b
      where b.template = :template
        and b.startsAt < :to
      order by b.startsAt asc
      """)
  Page<AvailabilityTemplateBlock> findByTemplateStartingBefore(
      AvailabilityTemplate template, OffsetDateTime to, Pageable pageable);

  @Query(
      """
      select b
      from AvailabilityTemplateBlock b
      where b.template = :template
        and b.endsAt > :from
        and b.startsAt < :to
      order by b.startsAt asc
      """)
  Page<AvailabilityTemplateBlock> findByTemplateOverlappingRange(
      AvailabilityTemplate template, OffsetDateTime from, OffsetDateTime to, Pageable pageable);

  @Query(
      """
      select b
      from AvailabilityTemplateBlock b
      where b.template = :template
        and b.endsAt > :from
        and b.startsAt < :to
      order by b.startsAt asc
      """)
  List<AvailabilityTemplateBlock> findByTemplateOverlappingRange(
      AvailabilityTemplate template, OffsetDateTime from, OffsetDateTime to);

  Optional<AvailabilityTemplateBlock> findByIdAndTemplate(UUID id, AvailabilityTemplate template);
}
