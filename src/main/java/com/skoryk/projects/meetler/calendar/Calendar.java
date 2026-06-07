package com.skoryk.projects.meetler.calendar;

import com.skoryk.projects.meetler.calendar.external.ExternalCalendarAccount;
import com.skoryk.projects.meetler.user.AppUser;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "calendar")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Calendar {

  @Id @GeneratedValue private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private AppUser user;

  @Column(nullable = false)
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private CalendarProvider provider;

  @Column private String color;

  @Column(name = "is_editable", nullable = false)
  private boolean isEditable;

  @Column(name = "is_active", nullable = false)
  private boolean isActive;

  @Column private String externalId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "external_calendar_account_id")
  private ExternalCalendarAccount externalCalendarAccount;

  @Enumerated(EnumType.STRING)
  @Column(name = "sync_direction", nullable = false)
  private CalendarSynchronizationType syncDirection;

  @CreationTimestamp
  @Column(nullable = false)
  private OffsetDateTime createdAt;

  @UpdateTimestamp
  @Column(nullable = false)
  private OffsetDateTime updatedAt;
}
