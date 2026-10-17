package com.bustrackpro.service.approval;

import com.bustrackpro.dto.request.approval.ApprovalRequestDTO;
import com.bustrackpro.dto.response.approval.ApprovalResponseDTO;
import com.bustrackpro.modal.approval.ApprovalStatus;
import com.bustrackpro.modal.approval.ApprovalType;

import java.util.List;

/**
 * Service interface for handling approval requests operations.
 */
public interface ApprovalService {

    /**
     * Creates a new approval request.
     *
     * @param requestDTO - DTO with request details
     * @return Created approval request response DTO
     */
    ApprovalResponseDTO createRequest(ApprovalRequestDTO requestDTO);

    /**
     * Gets a history of approval requests for a specific mobile user.
     *
     * @param userId - ID of the mobile user
     * @return List of approval requests
     */
    List<ApprovalResponseDTO> getUserRequests(Integer userId);

    /**
     * Gets all approval requests for admin with optional filters.
     *
     * @param status - optional status filter
     * @param type - optional type filter
     * @return List of filtered approval requests
     */
    List<ApprovalResponseDTO> getAdminRequests(ApprovalStatus status, ApprovalType type);

    /**
     * Updates the status of an approval request.
     *
     * @param id - ID of the approval request
     * @param status - new status (APPROVED/REJECTED)
     * @param reason - optional comment or reason
     * @return Updated approval request response DTO
     */
    ApprovalResponseDTO updateStatus(Integer id, ApprovalStatus status, String reason);
}
