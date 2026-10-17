package com.bustrackpro.dto.request.user;

import com.bustrackpro.modal.user.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Sign up request DTO.
 */
@Getter
@Setter
public class SignUpRequest {
    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String password;

    private String contactNumber;

    @NotNull
    private UserRole role;

    private String licenseNumber;
}
