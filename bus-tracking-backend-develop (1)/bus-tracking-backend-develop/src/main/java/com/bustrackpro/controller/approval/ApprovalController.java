package com.bustrackpro.controller.approval;

import com.bustrackpro.dto.request.approval.ApprovalRequestDTO;
import com.bustrackpro.dto.request.approval.ApprovalStatusUpdateDTO;
import com.bustrackpro.dto.response.approval.ApprovalResponseDTO;
import com.bustrackpro.modal.approval.ApprovalStatus;
import com.bustrackpro.modal.approval.ApprovalType;
import com.bustrackpro.service.approval.ApprovalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller for handling Approval Requests REST endpoints.
 */
@RestController
@RequestMapping("/approvals")
@RequiredArgsConstructor
public class ApprovalController {

    private final ApprovalService approvalService;

    /**
     * Create a new approval request.
     *
     * @param requestDTO - DTO containing request body
     * @return response containing created request
     */
    @PostMapping("/request")
    public ResponseEntity<ApprovalResponseDTO> createRequest(@Valid @RequestBody ApprovalRequestDTO requestDTO) {
        return ResponseEntity.status(HttpStatus.CREATED).body(approvalService.createRequest(requestDTO));
    }

    /**
     * Get history of requests for a specific mobile user.
     *
     * @param userId - path variable for mobile user ID
     * @return response containing list of requests
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ApprovalResponseDTO>> getUserRequests(@PathVariable Integer userId) {
        return ResponseEntity.ok(approvalService.getUserRequests(userId));
    }

    /**
     * Get all requests for admins, with optional filters.
     *
     * @param status - optional status query parameter (PENDING, etc.)
     * @param type - optional type query parameter (EMERGENCY, NORMAL)
     * @return response containing list of filtered requests
     */
    @GetMapping("/admin")
    public ResponseEntity<List<ApprovalResponseDTO>> getAdminRequests(
            @RequestParam(required = false) ApprovalStatus status,
            @RequestParam(required = false) ApprovalType type) {
        return ResponseEntity.ok(approvalService.getAdminRequests(status, type));
    }

    /**
     * Update the status of an approval request (APPROVED or REJECTED).
     *
     * @param id - path variable for request ID
     * @param updateDTO - request body containing new status and optional reason string
     * @return response containing updated request
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApprovalResponseDTO> updateStatus(
            @PathVariable Integer id,
            @Valid @RequestBody ApprovalStatusUpdateDTO updateDTO) {
        return ResponseEntity.ok(approvalService.updateStatus(id, updateDTO.getStatus(), updateDTO.getReason()));
    }
}
