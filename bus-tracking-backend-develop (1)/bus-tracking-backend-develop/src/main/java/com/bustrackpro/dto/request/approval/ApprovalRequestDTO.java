package com.bustrackpro.dto.request.approval;

import com.bustrackpro.modal.approval.ApprovalType;
import com.bustrackpro.modal.approval.EmergencyType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO for creating an approval request.
 */
@Data
public class ApprovalRequestDTO {

    @NotNull(message = "Requested by user ID is required")
    private Integer requestedById;

    @NotNull(message = "Bus ID is required")
    private Integer busId;

    @NotNull(message = "Approval type is required")
    private ApprovalType approvalType;

    // Emergency specific fields
    private EmergencyType emergencyType;

    // Normal specific fields
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalTime startTime;
    private LocalTime endTime;
}
