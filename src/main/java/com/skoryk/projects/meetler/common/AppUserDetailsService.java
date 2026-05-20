package com.skoryk.projects.meetler.common;

import com.skoryk.projects.meetler.auth.dto.AppUserDetails;
import com.skoryk.projects.meetler.user.AppUser;
import com.skoryk.projects.meetler.user.AppUserRepository;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@AllArgsConstructor
@Service
@Slf4j
public class AppUserDetailsService implements UserDetailsService {

  private AppUserRepository userRepository;

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    if (username.contains("@")) {
      AppUser user = userRepository.findByEmail(username).orElse(null);
      if (user == null) {
        log.error("User not found with username: {}", username);
        throw new UsernameNotFoundException("User not found with username: " + username);
      }
      log.info("User found: {}", user.getId());
      return new AppUserDetails(user);
    }

    return this.loadUserByUUID(UUID.fromString(username));
  }

  public AppUserDetails loadUserByUUID(UUID uuid) throws UsernameNotFoundException {
    // UUID is the username in this applicaiton
    AppUser user =
        userRepository
            .findById(uuid)
            .orElseThrow(
                () -> {
                  log.error("User not found with username: {}", uuid);
                  throw new UsernameNotFoundException("User not found with username: " + uuid);
                });

    log.info("User found: {}", user.getId());
    return new AppUserDetails(user);
  }
}
