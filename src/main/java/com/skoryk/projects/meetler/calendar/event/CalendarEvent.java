package com.skoryk.projects.meetler.calendar.event;

import com.skoryk.projects.meetler.calendar.Calendar;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(
    name = "calendar_event",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uq_calendar_event_calendar_external",
          columnNames = {"calendar_id", "external_id"})
    })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalendarEvent {

  @Id @GeneratedValue private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "calendar_id", nullable = false)
  private Calendar calendar;

  @Column(name = "external_id", nullable = false, length = 512)
  private String externalId;

  @Column(length = 512)
  private String title;

  @Column(name = "starts_at", nullable = false)
  private OffsetDateTime startsAt;

  @Column(name = "ends_at", nullable = false)
  private OffsetDateTime endsAt;

  @Column(name = "is_busy", nullable = false)
  private boolean busy;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;
}
