package com.bustrackpro.controller.auth;

import com.bustrackpro.dto.request.user.LoginRequest;
import com.bustrackpro.dto.request.user.SignUpRequest;
import com.bustrackpro.dto.response.user.ApiResponse;
import com.bustrackpro.dto.response.user.JwtAuthenticationResponse;
import com.bustrackpro.service.auth.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Auth controller.
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    /**
     * Instantiates a new Auth controller.
     */
    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Authenticates user.
     */
    @PostMapping("/sign-in")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        JwtAuthenticationResponse jwtAuthenticationResponse = authService.authenticateUser(loginRequest);
        return ResponseEntity.ok(jwtAuthenticationResponse);
    }

    /**
     * Registers user.
     */
    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignUpRequest signUpRequest) {
        boolean isRegistered = authService.registerUser(signUpRequest);
        
        if (!isRegistered) {
            return new ResponseEntity<>(new ApiResponse(false, "Email Address already in use!"),
                    HttpStatus.BAD_REQUEST);
        }

        return ResponseEntity.ok(new ApiResponse(true, "User registered successfully"));
    }

    /**
     * Validates a token.
     */
    @PostMapping("/validate-token")
    public ResponseEntity<?> validateToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            String jwt = bearerToken.substring(7);
            if (authService.validateToken(jwt)) {
                return ResponseEntity.ok(new ApiResponse(true, "Token is valid"));
            }
        }
        return new ResponseEntity<>(new ApiResponse(false, "Invalid token"), HttpStatus.UNAUTHORIZED);
    }
}
