package com.bustrackpro.dto.response.user;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * Api response.
 */
@Getter
@Setter
@AllArgsConstructor
public class ApiResponse {
    private Boolean success;
    private String message;
}
