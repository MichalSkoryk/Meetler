package com.skoryk.projects.meetler.auth.token;

import com.skoryk.projects.meetler.user.AppUser;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

  Optional<RefreshToken> findByToken(String token);

  void deleteByUser(AppUser user);
}
