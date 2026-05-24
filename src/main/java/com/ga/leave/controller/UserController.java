package com.ga.leave.controller;

import com.ga.leave.mapper.UserMapper;
import com.ga.leave.model.User;
import com.ga.leave.model.request.ChangePasswordRequest;
import com.ga.leave.model.request.LoginRequest;
import com.ga.leave.model.request.NewPasswordRequest;
import com.ga.leave.model.request.RegisterRequest;
import com.ga.leave.model.request.ResetPasswordRequest;
import com.ga.leave.model.response.ApiResponse;
import com.ga.leave.model.response.UserResponseDTO;
import com.ga.leave.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = "/api/v1/auth")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponseDTO>> createUser(@Valid @RequestBody RegisterRequest request) {
        User user = userService.createUser(request);
        UserResponseDTO dto = userMapper.toDTO(user);
        return ResponseEntity.ok(ApiResponse.success(dto, "User registered successfully. Please check your email to verify your account."));
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@Valid @RequestBody LoginRequest loginRequest) {
        return userService.loginUser(loginRequest);
    }

    @PutMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(request.getOldPass(), request.getNewPass());
        return ResponseEntity.ok(ApiResponse.success(null, "Password changed successfully"));
    }

    @GetMapping("/verify")
    public ResponseEntity<ApiResponse<Void>> validate(@RequestParam String token) {
        userService.validate(token);
        return ResponseEntity.ok(ApiResponse.success(null, "Account verified successfully"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> passwordReset(@Valid @RequestBody ResetPasswordRequest request) {
        userService.resetPassword(request.getEmailAddress());
        return ResponseEntity.ok(ApiResponse.success(null, "If the email exists, a reset link has been sent"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> passwordResetActivator(@Valid @RequestBody NewPasswordRequest request, @RequestParam String token) {
        userService.resetPasswordActivator(token, request);
        return ResponseEntity.ok(ApiResponse.success(null, "Password reset successfully"));
    }

    @DeleteMapping("/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> softDeleteUser(@PathVariable Long userId) {
        userService.softDeleteUser(userId);
        return ResponseEntity.ok(ApiResponse.success(null, "User deactivated successfully"));
    }

    @PutMapping("/users/{userId}/promote")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> promoteUserToAdmin(@PathVariable Long userId) {
        userService.promoteUserToAdmin(userId);
        return ResponseEntity.ok(ApiResponse.success(null, "User promoted to ADMIN successfully"));
    }

    @PutMapping("/users/{userId}/demote")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> demoteUserFromAdmin(@PathVariable Long userId) {
        userService.demoteUserFromAdmin(userId);
        return ResponseEntity.ok(ApiResponse.success(null, "User demoted from ADMIN successfully"));
    }

}
