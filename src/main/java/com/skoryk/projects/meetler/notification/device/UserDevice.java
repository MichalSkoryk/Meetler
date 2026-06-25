package com.skoryk.projects.meetler.notification.device;

import com.skoryk.projects.meetler.user.AppUser;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(
    name = "user_device",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uq_user_device_user_token",
          columnNames = {"user_id", "token"})
    })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDevice {

  @Id @GeneratedValue private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private AppUser user;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private UserDevicePlatform platform;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private UserDeviceProvider provider;

  @Column(nullable = false, length = 1000)
  private String token;

  @Column(nullable = false)
  private boolean enabled;

  @Column(name = "last_seen_at")
  private OffsetDateTime lastSeenAt;

  @Column(name = "revoked_at")
  private OffsetDateTime revokedAt;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;
}
