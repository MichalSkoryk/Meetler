package com.skoryk.projects.meetler.auth.guest;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GuestLoginTokenRepository extends JpaRepository<GuestLoginToken, UUID> {

  Optional<GuestLoginToken> findByTokenHash(String tokenHash);
}
