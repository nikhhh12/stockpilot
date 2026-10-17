package com.stockpilot.controller;

import com.stockpilot.dto.response.ApiResponse;
import com.stockpilot.dto.response.UserResponse;
import com.stockpilot.entity.RoleEnum;
import com.stockpilot.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User & Role Administration", description = "Privileged administrator operations for user governance")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;

    @GetMapping
    @Operation(summary = "List all registered users", description = "Admin-only view of all users and their roles")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers() {
        List<UserResponse> list = userService.getAllUsers();
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user details by ID")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long id) {
        UserResponse response = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}/role")
    @Operation(summary = "Update a user's role", description = "Promote or demote user authority (ADMIN, INVENTORY_MANAGER, STAFF)")
    public ResponseEntity<ApiResponse<UserResponse>> updateUserRole(
            @PathVariable Long id,
            @RequestBody Map<String, String> body
    ) {
        String roleStr = body.get("role");
        if (roleStr == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Role parameter is required"));
        }
        RoleEnum role = RoleEnum.valueOf(roleStr.toUpperCase());
        UserResponse response = userService.updateUserRole(id, role);
        return ResponseEntity.ok(ApiResponse.success(response, "User role successfully updated to " + role));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Enable or disable user account")
    public ResponseEntity<ApiResponse<UserResponse>> updateUserStatus(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> body
    ) {
        Boolean enabled = body.get("enabled");
        if (enabled == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Enabled boolean is required"));
        }
        UserResponse response = userService.updateUserStatus(id, enabled);
        return ResponseEntity.ok(ApiResponse.success(response, "User status updated to " + (enabled ? "ENABLED" : "DISABLED")));
    }
}
