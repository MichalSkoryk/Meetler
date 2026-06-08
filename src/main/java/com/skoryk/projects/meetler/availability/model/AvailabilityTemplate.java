package com.skoryk.projects.meetler.availability.model;

import com.skoryk.projects.meetler.user.AppUser;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "availability_template")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AvailabilityTemplate {

  @Id @GeneratedValue private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private AppUser user;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false, length = 64)
  private String timezone;

  @Column(name = "is_default", nullable = false)
  private boolean isDefault;

  @Enumerated(EnumType.STRING)
  @Column(name = "default_availability_status", nullable = false, length = 32)
  private AvailabilityBlockStatus defaultAvailabilityStatus;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;
}
