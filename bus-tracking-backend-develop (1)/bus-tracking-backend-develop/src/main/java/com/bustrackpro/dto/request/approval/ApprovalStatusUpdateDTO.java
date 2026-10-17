package com.bustrackpro.dto.request.approval;

import com.bustrackpro.modal.approval.ApprovalStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * DTO for updating an approval request's status.
 */
@Data
public class ApprovalStatusUpdateDTO {

    @NotNull(message = "Status is required")
    private ApprovalStatus status;

    private String reason;
}
