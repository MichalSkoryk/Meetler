package com.skoryk.projects.meetler.auth.token;

import com.skoryk.projects.meetler.user.AppUser;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

  Optional<RefreshToken> findByToken(String token);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select token from RefreshToken token join fetch token.user where token.token = :token")
  Optional<RefreshToken> findByTokenForUpdate(@Param("token") String token);

  void deleteByUser(AppUser user);
}
