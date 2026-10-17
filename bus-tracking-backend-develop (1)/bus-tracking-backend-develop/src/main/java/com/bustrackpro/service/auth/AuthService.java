package com.bustrackpro.service.auth;

import com.bustrackpro.dto.request.user.LoginRequest;
import com.bustrackpro.dto.request.user.SignUpRequest;
import com.bustrackpro.dto.response.user.JwtAuthenticationResponse;

/**
 * Auth service interface.
 */
public interface AuthService {

    /**
     * Authenticates user.
     */
    JwtAuthenticationResponse authenticateUser(LoginRequest loginRequest);

    /**
     * Registers user.
     */
    boolean registerUser(SignUpRequest signUpRequest);

    /**
     * Validates a token.
     */
    boolean validateToken(String token);
}
