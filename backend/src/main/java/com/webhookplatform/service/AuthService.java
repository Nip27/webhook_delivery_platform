package com.webhookplatform.service;

import com.webhookplatform.dto.request.LoginRequest;
import com.webhookplatform.dto.request.RegisterRequest;
import com.webhookplatform.dto.response.AuthResponse;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    AuthResponse refresh(String refreshToken);
}
