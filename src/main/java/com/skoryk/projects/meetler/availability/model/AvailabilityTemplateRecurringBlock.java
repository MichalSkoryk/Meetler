package com.skoryk.projects.meetler.availability.model;

import jakarta.persistence.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "availability_template_recurring_block")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AvailabilityTemplateRecurringBlock {

  @Id @GeneratedValue private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "template_id", nullable = false)
  private AvailabilityTemplate template;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private AvailabilityRecurrenceFrequency frequency;

  @Column(name = "interval_count", nullable = false)
  private Integer intervalCount;

  @Column(name = "occurrence_count")
  private Integer occurrenceCount;

  @Enumerated(EnumType.STRING)
  @Column(name = "day_of_week", length = 16)
  private DayOfWeek dayOfWeek;

  @Column(name = "day_of_month")
  private Integer dayOfMonth;

  @Column(name = "month_of_year")
  private Integer monthOfYear;

  @Column(name = "start_time", nullable = false)
  private LocalTime startTime;

  @Column(name = "end_time", nullable = false)
  private LocalTime endTime;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private AvailabilityBlockStatus status;

  @Column(length = 255)
  private String note;

  @Column(name = "starts_on")
  private LocalDate startsOn;

  @Column(name = "ends_on")
  private LocalDate endsOn;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;
}
