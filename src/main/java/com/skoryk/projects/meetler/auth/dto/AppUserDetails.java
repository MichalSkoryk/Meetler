package com.skoryk.projects.meetler.auth.dto;

import com.skoryk.projects.meetler.user.AppUser;
import java.util.Collection;
import java.util.Collections;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@AllArgsConstructor
public class AppUserDetails implements UserDetails {

  private final AppUser appUser;

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return Collections.singletonList(new SimpleGrantedAuthority(appUser.getRole().name()));
  }

  @Override
  public @Nullable String getPassword() {
    return appUser.getPasswordHash();
  }

  @Override
  public String getUsername() {
    return appUser.getEmail();
  }

  @Override
  public boolean isAccountNonExpired() {
    return appUser.getDeletedAt() == null;
  }

  @Override
  public boolean isAccountNonLocked() {
    return appUser.getDeletedAt() == null;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }

  @Override
  public boolean isEnabled() {
    return appUser.getDeletedAt() == null;
  }
}
