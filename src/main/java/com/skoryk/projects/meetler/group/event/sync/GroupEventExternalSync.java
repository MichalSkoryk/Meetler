package com.skoryk.projects.meetler.group.event.sync;

import com.skoryk.projects.meetler.calendar.CalendarProvider;
import com.skoryk.projects.meetler.calendar.external.ExternalCalendarAccount;
import com.skoryk.projects.meetler.group.event.GroupEvent;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(
    name = "group_event_external_sync",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uq_group_event_external_sync_event_account_provider",
          columnNames = {"group_event_id", "external_calendar_account_id", "provider"})
    })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupEventExternalSync {

  @Id @GeneratedValue private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "group_event_id", nullable = false)
  private GroupEvent groupEvent;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "external_calendar_account_id", nullable = false)
  private ExternalCalendarAccount externalCalendarAccount;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private CalendarProvider provider;

  @Column(name = "external_calendar_id", nullable = false)
  private String externalCalendarId;

  @Column(name = "external_event_id", nullable = false)
  private String externalEventId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private GroupEventExternalSyncStatus status;

  @Column(name = "last_synced_at", nullable = false)
  private OffsetDateTime lastSyncedAt;

  @Column(name = "last_error", length = 2000)
  private String lastError;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;
}
