package com.stockpilot.service;

import com.stockpilot.dto.request.LoginRequest;
import com.stockpilot.dto.request.RegisterRequest;
import com.stockpilot.dto.response.AuthResponse;
import com.stockpilot.dto.response.UserResponse;

public interface AuthService {
    AuthResponse login(LoginRequest request);
    AuthResponse register(RegisterRequest request);
    AuthResponse refreshToken(String token);
    UserResponse getCurrentUserProfile(String email);
}
