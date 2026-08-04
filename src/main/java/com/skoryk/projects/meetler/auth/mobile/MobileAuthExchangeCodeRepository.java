package com.skoryk.projects.meetler.auth.mobile;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MobileAuthExchangeCodeRepository
    extends JpaRepository<MobileAuthExchangeCode, UUID> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select code from MobileAuthExchangeCode code where code.codeHash = :codeHash")
  Optional<MobileAuthExchangeCode> findForUpdateByCodeHash(@Param("codeHash") String codeHash);
}
