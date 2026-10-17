package com.bustrackpro.dto.request.user;

import com.bustrackpro.modal.user.UserRole;
import lombok.Getter;
import lombok.Setter;

/**
 * Update user request.
 */
@Getter
@Setter
public class UpdateUserRequest {
    private String firstName;
    private String lastName;
    private String contactNumber;
    private UserRole role;
    private String status;
}
