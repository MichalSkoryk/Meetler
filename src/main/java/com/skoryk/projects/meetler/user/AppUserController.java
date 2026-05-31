package com.skoryk.projects.meetler.user;

import com.skoryk.projects.meetler.user.dto.CreateUserRequest;
import com.skoryk.projects.meetler.user.dto.UpdateNameRequest;
import com.skoryk.projects.meetler.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "User Management")
@Validated
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class AppUserController {

  private final AppUserService appUserService;

  @PostMapping
  public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
    AppUser appUser = appUserService.createUser(request.getEmail(), request.getAuthProvider());
    return ResponseEntity.ok(UserResponse.from(appUser));
  }

  @GetMapping("/{id}")
  public ResponseEntity<UserResponse> getUser(@PathVariable UUID id) {
    return appUserService
        .findById(id)
        .map(appUser -> ResponseEntity.ok(UserResponse.from(appUser)))
        .orElse(ResponseEntity.notFound().build());
  }

  @GetMapping("/by-email")
  public ResponseEntity<UserResponse> getUserByEmail(@Email @RequestParam String email) {
    return appUserService
        .findByEmail(email)
        .map(appUser -> ResponseEntity.ok(UserResponse.from(appUser)))
        .orElse(ResponseEntity.notFound().build());
  }

  @PatchMapping("/{id}/name")
  public ResponseEntity<UserResponse> updateName(
      @PathVariable UUID id, @Valid @RequestBody UpdateNameRequest request) {
    AppUser updated = appUserService.updateName(id, request.getName());
    return ResponseEntity.ok(UserResponse.from(updated));
  }

  @PostMapping("/{id}/upgrade")
  public ResponseEntity<UserResponse> upgradeUser(@PathVariable UUID id) {
    AppUser upgraded = appUserService.upgradeToUser(id);
    return ResponseEntity.ok(UserResponse.from(upgraded));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> softDelete(@PathVariable UUID id) {
    appUserService.softDelete(id);
    return ResponseEntity.noContent().build();
  }
}
