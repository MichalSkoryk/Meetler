package com.skoryk.projects.meetler.auth.identity;

import com.skoryk.projects.meetler.user.AppUser;
import com.skoryk.projects.meetler.user.AuthProvider;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(
    name = "user_auth_identity",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uq_user_auth_identity_provider_user",
          columnNames = {"provider", "provider_user_id"})
    })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAuthIdentity {

  @Id @GeneratedValue private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private AppUser user;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private AuthProvider provider;

  @Column(name = "provider_user_id", nullable = false)
  private String providerUserId;

  @Column(name = "provider_email")
  private String providerEmail;

  @CreationTimestamp
  @Column(nullable = false)
  private OffsetDateTime createdAt;

  @Column(name = "last_login_at")
  private OffsetDateTime lastLoginAt;
}
