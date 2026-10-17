package com.stockpilot.service.impl;

import com.stockpilot.dto.response.UserResponse;
import com.stockpilot.entity.RoleEnum;
import com.stockpilot.entity.User;
import com.stockpilot.exception.ResourceNotFoundException;
import com.stockpilot.mapper.UserMapper;
import com.stockpilot.repository.UserRepository;
import com.stockpilot.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final com.stockpilot.service.AuditLogService auditLogService;

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(userMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional
    public UserResponse updateUserRole(Long id, RoleEnum newRole) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        user.setRole(newRole);
        User saved = userRepository.save(user);
        auditLogService.logAction("UPDATE_USER_ROLE", "User", saved.getId(), "Updated user role for " + saved.getEmail() + " to " + newRole);
        return userMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public UserResponse updateUserStatus(Long id, boolean enabled) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        user.setEnabled(enabled);
        User saved = userRepository.save(user);
        auditLogService.logAction("UPDATE_USER_STATUS", "User", saved.getId(), "Updated user status for " + saved.getEmail() + " enabled=" + enabled);
        return userMapper.toResponse(saved);
    }
}
