package com.bustrackpro.dto.response.user;

import com.bustrackpro.modal.user.UserRole;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * User response.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private int id;
    private String firstName;
    private String lastName;
    private String email;
    private String contactNumber;
    private UserRole role;
    private String status;
}
