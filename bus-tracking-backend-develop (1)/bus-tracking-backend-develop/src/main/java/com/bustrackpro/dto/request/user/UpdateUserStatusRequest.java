package com.bustrackpro.dto.request.user;

import lombok.Getter;
import lombok.Setter;

/**
 * Update user status request.
 */
@Getter
@Setter
public class UpdateUserStatusRequest {
    private String status;
}
