package com.skoryk.projects.meetler.notification.delivery;

import com.skoryk.projects.meetler.notification.Notification;
import com.skoryk.projects.meetler.notification.device.UserDevice;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "notification_delivery")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDelivery {

  @Id @GeneratedValue private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "notification_id", nullable = false)
  private Notification notification;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "device_id")
  private UserDevice device;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40)
  private NotificationChannel channel;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40)
  private NotificationDeliveryStatus status;

  @Column(name = "last_error", length = 2000)
  private String lastError;

  @Column(name = "sent_at")
  private OffsetDateTime sentAt;

  @Builder.Default
  @Column(name = "attempt_count", nullable = false)
  private int attemptCount = 0;

  @Column(name = "last_attempt_at")
  private OffsetDateTime lastAttemptAt;

  @Column(name = "next_attempt_at")
  private OffsetDateTime nextAttemptAt;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;
}
