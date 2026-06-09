package com.skoryk.projects.meetler.auth.password;

import com.skoryk.projects.meetler.user.AppUser;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "password_reset_token")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetToken {

  @Id @GeneratedValue private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private AppUser user;

  @Column(name = "token_hash", nullable = false, unique = true)
  private String tokenHash;

  @Column(nullable = false)
  private OffsetDateTime expiresAt;

  private OffsetDateTime usedAt;

  @CreationTimestamp
  @Column(nullable = false)
  private OffsetDateTime createdAt;
}
