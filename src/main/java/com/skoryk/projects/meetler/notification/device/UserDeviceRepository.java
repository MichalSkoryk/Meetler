package com.skoryk.projects.meetler.notification.device;

import com.skoryk.projects.meetler.user.AppUser;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserDeviceRepository extends JpaRepository<UserDevice, UUID> {

  Optional<UserDevice> findByUserAndToken(AppUser user, String token);

  List<UserDevice> findByUserAndEnabledTrueAndRevokedAtIsNull(AppUser user);
}
