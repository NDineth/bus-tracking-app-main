package com.bustrackpro.dto.response.approval;

import com.bustrackpro.modal.approval.ApprovalStatus;
import com.bustrackpro.modal.approval.ApprovalType;
import com.bustrackpro.modal.approval.EmergencyType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * DTO for sending approval request details in responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalResponseDTO {
    private Integer id;
    private Integer requestedById;
    private String requestedByName;
    private Integer busId;
    private String busNumber;
    private ApprovalStatus status;
    private ApprovalType approvalType;
    private EmergencyType emergencyType;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String reason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
