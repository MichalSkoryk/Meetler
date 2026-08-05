package com.skoryk.projects.meetler.user;

import com.skoryk.projects.meetler.user.dto.*;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AppUserController implements AppUserApi {

  private final AppUserService appUserService;
  private final AppUserMapper appUserMapper;

  @Override
  public ResponseEntity<UserResponse> createUser(CreateUserRequest request) {
    AppUser appUser = appUserService.createUser(request.getEmail());
    return ResponseEntity.ok(appUserMapper.toResponse(appUser));
  }

  @Override
  public ResponseEntity<UserResponse> getUser(UUID id) {
    return appUserService
        .findById(id)
        .map(appUser -> ResponseEntity.ok(appUserMapper.toResponse(appUser)))
        .orElse(ResponseEntity.notFound().build());
  }

  @Override
  public ResponseEntity<UserResponse> getUserByEmail(String email) {
    return appUserService
        .findByEmail(email)
        .map(appUser -> ResponseEntity.ok(appUserMapper.toResponse(appUser)))
        .orElse(ResponseEntity.notFound().build());
  }

  @Override
  public ResponseEntity<UserResponse> updateName(UUID id, UpdateNameRequest request) {
    AppUser updated = appUserService.updateName(id, request.getName());
    return ResponseEntity.ok(appUserMapper.toResponse(updated));
  }

  @Override
  public ResponseEntity<UserResponse> upgradeUser(UUID id) {
    AppUser upgraded = appUserService.upgradeToUser(id);
    return ResponseEntity.ok(appUserMapper.toResponse(upgraded));
  }

  @Override
  public ResponseEntity<Void> softDelete(UUID id) {
    appUserService.softDelete(id);
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<Void> changePassword(AppUser user, ChangePasswordRequest request) {
    appUserService.changePassword(user, request.getCurrentPassword(), request.getNewPassword());
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<Void> setInitialPassword(AppUser user, SetInitialPasswordRequest request) {
    appUserService.setInitialPassword(user, request.getInitialPassword());
    return ResponseEntity.noContent().build();
  }
}
