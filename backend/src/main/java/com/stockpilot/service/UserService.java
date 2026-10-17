package com.stockpilot.service;

import com.stockpilot.dto.response.UserResponse;
import com.stockpilot.entity.RoleEnum;

import java.util.List;

public interface UserService {
    List<UserResponse> getAllUsers();
    UserResponse getUserById(Long id);
    UserResponse updateUserRole(Long id, RoleEnum newRole);
    UserResponse updateUserStatus(Long id, boolean enabled);
}
