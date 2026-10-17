package com.bustrackpro.service.approval;

import com.bustrackpro.dto.request.approval.ApprovalRequestDTO;
import com.bustrackpro.dto.response.approval.ApprovalResponseDTO;
import com.bustrackpro.modal.NotificationCategory;
import com.bustrackpro.modal.approval.ApprovalRequest;
import com.bustrackpro.modal.approval.ApprovalStatus;
import com.bustrackpro.modal.approval.ApprovalType;
import com.bustrackpro.modal.bus.Bus;
import com.bustrackpro.modal.user.UserRole;
import com.bustrackpro.modal.user.Users;
import com.bustrackpro.repository.approval.ApprovalRequestRepository;
import com.bustrackpro.repository.bus.BusRepository;
import com.bustrackpro.repository.user.UserRepository;
import com.bustrackpro.service.notification.NotificationService;
import com.bustrackpro.service.schedule.ScheduleAssignmentService;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Implementation of ApprovalService.
 */
@Service
@RequiredArgsConstructor
public class ApprovalServiceImpl implements ApprovalService {

    private final ApprovalRequestRepository approvalRequestRepository;
    private final UserRepository userRepository;
    private final BusRepository busRepository;
    private final NotificationService notificationService;
    private final ScheduleAssignmentService scheduleAssignmentService;

    @Override
    @Transactional
    public ApprovalResponseDTO createRequest(ApprovalRequestDTO requestDTO) {
        // Validate user
        Users user = userRepository.findById(requestDTO.getRequestedById())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with ID: " + requestDTO.getRequestedById()));

        // Validate bus
        Bus bus = busRepository.findById(requestDTO.getBusId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bus not found with ID: " + requestDTO.getBusId()));

        // Conditional validation based on ApprovalType
        if (requestDTO.getApprovalType() == ApprovalType.EMERGENCY) {
            if (requestDTO.getEmergencyType() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Emergency type is required for emergency approvals");
            }
        } else if (requestDTO.getApprovalType() == ApprovalType.NORMAL) {
            if (requestDTO.getStartDate() == null || requestDTO.getEndDate() == null
                    || requestDTO.getStartTime() == null || requestDTO.getEndTime() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Start date, end date, start time, and end time are required for normal approvals");
            }
        }

        // Map and save request
        ApprovalRequest request = ApprovalRequest.builder()
                .requestedBy(user)
                .bus(bus)
                .status(ApprovalStatus.PENDING)
                .approvalType(requestDTO.getApprovalType())
                .emergencyType(requestDTO.getApprovalType() == ApprovalType.EMERGENCY ? requestDTO.getEmergencyType() : null)
                .startDate(requestDTO.getApprovalType() == ApprovalType.NORMAL ? requestDTO.getStartDate() : null)
                .endDate(requestDTO.getApprovalType() == ApprovalType.NORMAL ? requestDTO.getEndDate() : null)
                .startTime(requestDTO.getApprovalType() == ApprovalType.NORMAL ? requestDTO.getStartTime() : null)
                .endTime(requestDTO.getApprovalType() == ApprovalType.NORMAL ? requestDTO.getEndTime() : null)
                .build();

        ApprovalRequest savedRequest = approvalRequestRepository.save(request);

        // Interconnected Logic (Notifications)
        // Trigger notification to Web App (Admin/Authority)
        triggerAdminNotification(savedRequest);

        return mapToResponse(savedRequest);
    }

    @Override
    public List<ApprovalResponseDTO> getUserRequests(Integer userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with ID: " + userId);
        }
        return approvalRequestRepository.findByRequestedById(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ApprovalResponseDTO> getAdminRequests(ApprovalStatus status, ApprovalType type) {
        return approvalRequestRepository.findByStatusAndApprovalTypeOptional(status, type).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ApprovalResponseDTO updateStatus(Integer id, ApprovalStatus status, String reason) {
        if (status != ApprovalStatus.APPROVED && status != ApprovalStatus.REJECTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Admins can only update status to APPROVED or REJECTED");
        }

        ApprovalRequest request = approvalRequestRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Approval request not found with ID: " + id));

        request.setStatus(status);
        request.setReason(reason);

        ApprovalRequest updatedRequest = approvalRequestRepository.save(request);
        // If the request gets approved, run the rescheduling logic
        if (status == ApprovalStatus.APPROVED) {
            if (updatedRequest.getApprovalType() == ApprovalType.EMERGENCY) {
                LocalDate requestedDay = updatedRequest.getCreatedAt() != null
                        ? updatedRequest.getCreatedAt().toLocalDate() 
                        : LocalDate.now();
                scheduleAssignmentService.rescheduleEmergency(
                        updatedRequest.getBus(),
                        requestedDay
                );
            } else if (updatedRequest.getApprovalType() == ApprovalType.NORMAL) {
                scheduleAssignmentService.rescheduleNormal(
                        updatedRequest.getBus(),
                        updatedRequest.getStartDate(),
                        updatedRequest.getEndDate(),
                        updatedRequest.getStartTime(),
                        updatedRequest.getEndTime()
                );
            }
        }

        // Interconnected Logic (Notifications)
        // Trigger push notification to Mobile App user who created the request
        triggerMobileNotification(updatedRequest, reason);

        return mapToResponse(updatedRequest);
    }

    private void triggerAdminNotification(ApprovalRequest request) {
        try {
            List<Integer> adminAndAuthorityIds = new ArrayList<>();
            userRepository.findByRole(UserRole.ADMIN).forEach(u -> adminAndAuthorityIds.add(u.getId()));
            userRepository.findByRole(UserRole.BUS_AUTHORITY_USER).forEach(u -> adminAndAuthorityIds.add(u.getId()));

            List<Integer> distinctReceiverIds = adminAndAuthorityIds.stream()
                    .distinct()
                    .collect(Collectors.toList());

            if (distinctReceiverIds.isEmpty()) {
                return;
            }

            String requesterName = request.getRequestedBy().getFirstName() + " " + request.getRequestedBy().getLastName();
            String title = "New Approval Request - Bus " + request.getBus().getBusNumber();
            String body = String.format("A new %s request has been submitted by %s.",
                    request.getApprovalType(), requesterName);

            notificationService.createNotification(
                    title,
                    body,
                    NotificationCategory.SYSTEM_ALERT,
                    "SYSTEM",
                    distinctReceiverIds
            );
        } catch (Exception e) {
            // Log warning but don't fail transaction for notification failures
            System.err.println("Failed to send admin notification: " + e.getMessage());
        }
    }

    private void triggerMobileNotification(ApprovalRequest request, String reason) {
        try {
            if (request.getRequestedBy() == null) {
                return;
            }

            Integer receiverId = request.getRequestedBy().getId();
            String statusStr = request.getStatus().name();
            String title = "Approval Request " + statusStr;
            
            StringBuilder bodyBuilder = new StringBuilder();
            bodyBuilder.append("Your approval request for Bus ").append(request.getBus().getBusNumber());
            bodyBuilder.append(" has been ").append(statusStr.toLowerCase()).append(".");
            if (reason != null && !reason.trim().isEmpty()) {
                bodyBuilder.append(" Reason: ").append(reason);
            }

            notificationService.createNotification(
                    title,
                    bodyBuilder.toString(),
                    NotificationCategory.USER_UPDATE,
                    "SYSTEM",
                    List.of(receiverId)
            );
        } catch (Exception e) {
            // Log warning but don't fail transaction for notification failures
            System.err.println("Failed to send driver/owner notification: " + e.getMessage());
        }
    }

    private ApprovalResponseDTO mapToResponse(ApprovalRequest request) {
        String requestedByName = "";
        if (request.getRequestedBy() != null) {
            requestedByName = request.getRequestedBy().getFirstName() + " " + request.getRequestedBy().getLastName();
        }

        return ApprovalResponseDTO.builder()
                .id(request.getId())
                .requestedById(request.getRequestedBy() != null ? request.getRequestedBy().getId() : null)
                .requestedByName(requestedByName)
                .busId(request.getBus() != null ? request.getBus().getId() : null)
                .busNumber(request.getBus() != null ? request.getBus().getBusNumber() : null)
                .status(request.getStatus())
                .approvalType(request.getApprovalType())
                .emergencyType(request.getEmergencyType())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .reason(request.getReason())
                .createdAt(request.getCreatedAt())
                .updatedAt(request.getUpdatedAt())
                .build();
    }
}
