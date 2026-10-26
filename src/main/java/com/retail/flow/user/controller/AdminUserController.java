package com.retail.flow.user.controller;

import com.retail.flow.user.dto.UserResponseDto;
import com.retail.flow.user.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    public ResponseEntity<List<UserResponseDto>> getAllUsers() {
        return ResponseEntity.ok(adminUserService.getAllUsers());
    }

    @PatchMapping("/{id}/role")
    public ResponseEntity<UserResponseDto> updateUserRole(
            @PathVariable Long id,
            @RequestParam String role,
            Authentication authentication) {
        return ResponseEntity.ok(adminUserService.updateUserRole(id, role, authentication.getName()));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<UserResponseDto> updateUserStatus(
            @PathVariable Long id,
            @RequestParam boolean active,
            Authentication authentication) {
        return ResponseEntity.ok(adminUserService.updateUserStatus(id, active, authentication.getName()));
    }
}