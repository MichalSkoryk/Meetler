package com.skoryk.projects.meetler.user;

import com.skoryk.projects.meetler.user.dto.ChangePasswordRequest;
import com.skoryk.projects.meetler.user.dto.CreateUserRequest;
import com.skoryk.projects.meetler.user.dto.SetInitialPasswordRequest;
import com.skoryk.projects.meetler.user.dto.UpdateNameRequest;
import com.skoryk.projects.meetler.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "User Management")
@Validated
@RequestMapping("/api/users")
public interface AppUserApi {

  @Operation(
      summary = "Create a user shell",
      description =
          "Creates a user record by email. This endpoint is useful for user administration flows.")
  @PostMapping
  ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request);

  @Operation(summary = "Get user by id", description = "Returns one user by id if it exists.")
  @GetMapping("/{id}")
  ResponseEntity<UserResponse> getUser(@PathVariable UUID id);

  @Operation(
      summary = "Get user by email",
      description = "Returns one user by email address if it exists.")
  @GetMapping("/by-email")
  ResponseEntity<UserResponse> getUserByEmail(@Email @RequestParam String email);

  @Operation(summary = "Update user name", description = "Updates the display name of a user.")
  @PatchMapping("/{id}/name")
  ResponseEntity<UserResponse> updateName(
      @PathVariable UUID id, @Valid @RequestBody UpdateNameRequest request);

  @Operation(
      summary = "Upgrade user account",
      description = "Upgrades a user record to a full active user account.")
  @PostMapping("/{id}/upgrade")
  ResponseEntity<UserResponse> upgradeUser(@PathVariable UUID id);

  @Operation(
      summary = "Soft-delete user",
      description = "Marks a user as deleted without physically removing the database record.")
  @DeleteMapping("/{id}")
  ResponseEntity<Void> softDelete(@PathVariable UUID id);

  @Operation(
      summary = "Change current password",
      description =
          "Changes the authenticated user's password. The current password must be supplied.")
  @PatchMapping("/me/password")
  ResponseEntity<Void> changePassword(
      @AuthenticationPrincipal AppUser user, @Valid @RequestBody ChangePasswordRequest request);

  @Operation(
      summary = "Set initial password",
      description =
          "Sets the authenticated user's first local password when the account does not already have one.")
  @PostMapping("/me/password")
  ResponseEntity<Void> setInitialPassword(
      @AuthenticationPrincipal AppUser user, @Valid @RequestBody SetInitialPasswordRequest request);
}
