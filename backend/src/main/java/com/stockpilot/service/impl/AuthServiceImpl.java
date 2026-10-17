package com.stockpilot.service.impl;

import com.stockpilot.dto.request.LoginRequest;
import com.stockpilot.dto.request.RegisterRequest;
import com.stockpilot.dto.response.AuthResponse;
import com.stockpilot.dto.response.UserResponse;
import com.stockpilot.entity.User;
import com.stockpilot.exception.DuplicateResourceException;
import com.stockpilot.exception.ResourceNotFoundException;
import com.stockpilot.mapper.UserMapper;
import com.stockpilot.repository.UserRepository;
import com.stockpilot.security.JwtTokenProvider;
import com.stockpilot.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final UserMapper userMapper;
    private final com.stockpilot.service.AuditLogService auditLogService;

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.getPassword())
        );

        String token = tokenProvider.generateToken(authentication);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        auditLogService.logAction(user, "LOGIN", "User", user.getId(), "User logged in: " + user.getEmail());

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .user(userMapper.toResponse(user))
                .build();
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("User with email '" + email + "' already exists");
        }

        com.stockpilot.entity.RoleEnum assignedRole = com.stockpilot.entity.RoleEnum.STAFF;
        java.util.Optional<String> currentUserEmail = com.stockpilot.security.SecurityUtils.getCurrentUserEmail();
        if (currentUserEmail.isPresent()) {
            User currentCaller = userRepository.findByEmail(currentUserEmail.get()).orElse(null);
            if (currentCaller != null && currentCaller.getRole() == com.stockpilot.entity.RoleEnum.ADMIN && request.getRole() != null) {
                assignedRole = request.getRole();
            }
        }

        User user = User.builder()
                .name(request.getName().trim())
                .email(email)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(assignedRole)
                .enabled(true)
                .build();

        User saved = userRepository.save(user);
        auditLogService.logAction(saved, "REGISTER", "User", saved.getId(), "User registered with role: " + saved.getRole());
        String token = tokenProvider.generateTokenFromUsername(saved.getEmail());

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .user(userMapper.toResponse(saved))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse refreshToken(String token) {
        if (tokenProvider.validateToken(token)) {
            String username = tokenProvider.getUsernameFromToken(token);
            User user = userRepository.findByEmail(username)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "email", username));

            String newToken = tokenProvider.generateTokenFromUsername(username);
            return AuthResponse.builder()
                    .token(newToken)
                    .tokenType("Bearer")
                    .user(userMapper.toResponse(user))
                    .build();
        } else {
            throw new ResourceNotFoundException("Invalid or expired token");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUserProfile(String email) {
        User user = userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
        return userMapper.toResponse(user);
    }
}
