package com.skoryk.projects.meetler.auth.mobile;

import com.skoryk.projects.meetler.user.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "mobile_auth_exchange_code")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MobileAuthExchangeCode {

  @Id @GeneratedValue private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private AppUser user;

  @Column(name = "code_hash", nullable = false, unique = true, length = 64)
  private String codeHash;

  @Column(name = "expires_at", nullable = false)
  private OffsetDateTime expiresAt;

  @Column(name = "used_at")
  private OffsetDateTime usedAt;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;
}
