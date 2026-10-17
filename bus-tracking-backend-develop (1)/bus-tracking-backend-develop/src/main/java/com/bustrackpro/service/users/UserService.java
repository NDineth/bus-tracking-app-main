package com.bustrackpro.service.users;

import com.bustrackpro.dto.request.user.UpdateRequest;
import com.bustrackpro.dto.request.user.UpdateUserStatusRequest;
import com.bustrackpro.dto.response.user.UserResponse;
import com.bustrackpro.modal.user.UserRole;

import java.util.List;

/**
 * User service interface.
 */
public interface UserService {
    
    /**
     * Gets all users.
     */
    List<UserResponse> getAllUsers();

    /**
     * Gets users by role.
     */
    List<UserResponse> getUsersByRole(UserRole role);

    /**
     * Updates a user's status.
     */
    UserResponse updateUserStatus(Integer id, UpdateUserStatusRequest request);

    /**
     * Updates a user.
     */
    UserResponse updateUser(Integer id, UpdateRequest updateRequest);

    /**
     * Deletes a user.
     */
    void deleteUser(Integer id);
}
