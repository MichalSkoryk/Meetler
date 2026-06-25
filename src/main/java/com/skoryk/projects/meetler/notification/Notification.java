package com.skoryk.projects.meetler.notification;

import com.skoryk.projects.meetler.user.AppUser;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "notification")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

  @Id @GeneratedValue private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private AppUser user;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 80)
  private NotificationType type;

  @Column(nullable = false, length = 160)
  private String title;

  @Column(nullable = false, length = 1000)
  private String body;

  @Column(name = "group_id")
  private UUID groupId;

  @Column(name = "event_id")
  private UUID eventId;

  @Column(name = "read_at")
  private OffsetDateTime readAt;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;
}
