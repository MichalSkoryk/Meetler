package com.skoryk.projects.meetler.availability.model;

import com.skoryk.projects.meetler.calendar.Calendar;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "availability_template_source_calendar")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AvailabilityTemplateSourceCalendar {

  @Id @GeneratedValue private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "template_id", nullable = false)
  private AvailabilityTemplate template;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "calendar_id", nullable = false)
  private Calendar calendar;

  @Column(name = "include_busy_events", nullable = false)
  private boolean includeBusyEvents;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;
}
