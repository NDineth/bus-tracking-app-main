package com.bustrackpro.dto.response.user;

import com.bustrackpro.modal.user.UserRole;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * Jwt authentication response.
 */
@Getter
@Setter
@RequiredArgsConstructor
public class JwtAuthenticationResponse {
    private final String accessToken;
    private String tokenType = "Bearer";
    private final String user;
    private final Integer userId;
    private final UserRole role;
}
