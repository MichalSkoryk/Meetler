package com.skoryk.projects.meetler.availability.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "availability_template_block")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AvailabilityTemplateBlock {

  @Id @GeneratedValue private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "template_id", nullable = false)
  private AvailabilityTemplate template;

  @Column(name = "starts_at", nullable = false)
  private OffsetDateTime startsAt;

  @Column(name = "ends_at", nullable = false)
  private OffsetDateTime endsAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private AvailabilityBlockStatus status;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private AvailabilityBlockSource source;

  @Column(length = 255)
  private String note;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;
}


