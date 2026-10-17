package com.bustrackpro.service.approval;

import com.bustrackpro.dto.request.approval.ApprovalRequestDTO;
import com.bustrackpro.dto.request.approval.ApprovalStatusUpdateDTO;
import com.bustrackpro.dto.response.approval.ApprovalResponseDTO;
import com.bustrackpro.modal.NotificationCategory;
import com.bustrackpro.modal.approval.ApprovalRequest;
import com.bustrackpro.modal.approval.ApprovalStatus;
import com.bustrackpro.modal.approval.ApprovalType;
import com.bustrackpro.modal.approval.EmergencyType;
import com.bustrackpro.modal.bus.Bus;
import com.bustrackpro.modal.user.UserRole;
import com.bustrackpro.modal.user.Users;
import com.bustrackpro.repository.approval.ApprovalRequestRepository;
import com.bustrackpro.repository.bus.BusRepository;
import com.bustrackpro.repository.user.UserRepository;
import com.bustrackpro.service.notification.NotificationService;
import com.bustrackpro.service.schedule.ScheduleAssignmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ApprovalServiceImplTest {

    @Mock
    private ApprovalRequestRepository approvalRequestRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BusRepository busRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private ScheduleAssignmentService scheduleAssignmentService;

    @InjectMocks
    private ApprovalServiceImpl approvalService;

    private Users testDriver;
    private Users testAdmin;
    private Bus testBus;
    private ApprovalRequest testEmergencyRequest;
    private ApprovalRequest testNormalRequest;

    @BeforeEach
    void setUp() {
        testDriver = new Users();
        testDriver.setId(1);
        testDriver.setFirstName("John");
        testDriver.setLastName("Doe");
        testDriver.setEmail("john.doe@test.com");
        testDriver.setRole(UserRole.BUS_DRIVER);

        testAdmin = new Users();
        testAdmin.setId(2);
        testAdmin.setFirstName("Admin");
        testAdmin.setLastName("User");
        testAdmin.setEmail("admin@test.com");
        testAdmin.setRole(UserRole.ADMIN);

        testBus = new Bus();
        testBus.setId(101);
        testBus.setBusNumber("WP-1234");
        testBus.setDriver(testDriver);

        testEmergencyRequest = ApprovalRequest.builder()
                .id(10)
                .requestedBy(testDriver)
                .bus(testBus)
                .status(ApprovalStatus.PENDING)
                .approvalType(ApprovalType.EMERGENCY)
                .emergencyType(EmergencyType.ACCIDENT)
                .build();

        testNormalRequest = ApprovalRequest.builder()
                .id(20)
                .requestedBy(testDriver)
                .bus(testBus)
                .status(ApprovalStatus.PENDING)
                .approvalType(ApprovalType.NORMAL)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(2))
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(17, 0))
                .build();
    }

    @Test
    void createRequest_ValidEmergency_Success() {
        // Arrange
        ApprovalRequestDTO requestDTO = new ApprovalRequestDTO();
        requestDTO.setRequestedById(1);
        requestDTO.setBusId(101);
        requestDTO.setApprovalType(ApprovalType.EMERGENCY);
        requestDTO.setEmergencyType(EmergencyType.ACCIDENT);

        when(userRepository.findById(1)).thenReturn(Optional.of(testDriver));
        when(busRepository.findById(101)).thenReturn(Optional.of(testBus));
        when(approvalRequestRepository.save(any(ApprovalRequest.class))).thenReturn(testEmergencyRequest);
        when(userRepository.findByRole(UserRole.ADMIN)).thenReturn(List.of(testAdmin));
        when(userRepository.findByRole(UserRole.BUS_AUTHORITY_USER)).thenReturn(Collections.emptyList());

        // Act
        ApprovalResponseDTO response = approvalService.createRequest(requestDTO);

        // Assert
        assertNotNull(response);
        assertEquals(ApprovalStatus.PENDING, response.getStatus());
        assertEquals(ApprovalType.EMERGENCY, response.getApprovalType());
        assertEquals(EmergencyType.ACCIDENT, response.getEmergencyType());
        assertEquals("WP-1234", response.getBusNumber());
        assertEquals("John Doe", response.getRequestedByName());

        verify(approvalRequestRepository).save(any(ApprovalRequest.class));
        verify(notificationService).createNotification(
                eq("New Approval Request - Bus WP-1234"),
                contains("EMERGENCY request has been submitted by John Doe"),
                eq(NotificationCategory.SYSTEM_ALERT),
                eq("SYSTEM"),
                eq(List.of(2))
        );
    }

    @Test
    void createRequest_InvalidEmergency_ThrowsException() {
        // Arrange
        ApprovalRequestDTO requestDTO = new ApprovalRequestDTO();
        requestDTO.setRequestedById(1);
        requestDTO.setBusId(101);
        requestDTO.setApprovalType(ApprovalType.EMERGENCY);
        requestDTO.setEmergencyType(null); // Invalid: missing emergencyType

        when(userRepository.findById(1)).thenReturn(Optional.of(testDriver));
        when(busRepository.findById(101)).thenReturn(Optional.of(testBus));

        // Act & Assert
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            approvalService.createRequest(requestDTO);
        });

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("Emergency type is required for emergency approvals", ex.getReason());
        verify(approvalRequestRepository, never()).save(any(ApprovalRequest.class));
    }

    @Test
    void createRequest_ValidNormal_Success() {
        // Arrange
        ApprovalRequestDTO requestDTO = new ApprovalRequestDTO();
        requestDTO.setRequestedById(1);
        requestDTO.setBusId(101);
        requestDTO.setApprovalType(ApprovalType.NORMAL);
        requestDTO.setStartDate(LocalDate.now());
        requestDTO.setEndDate(LocalDate.now().plusDays(2));
        requestDTO.setStartTime(LocalTime.of(8, 0));
        requestDTO.setEndTime(LocalTime.of(17, 0));

        when(userRepository.findById(1)).thenReturn(Optional.of(testDriver));
        when(busRepository.findById(101)).thenReturn(Optional.of(testBus));
        when(approvalRequestRepository.save(any(ApprovalRequest.class))).thenReturn(testNormalRequest);
        when(userRepository.findByRole(UserRole.ADMIN)).thenReturn(List.of(testAdmin));
        when(userRepository.findByRole(UserRole.BUS_AUTHORITY_USER)).thenReturn(Collections.emptyList());

        // Act
        ApprovalResponseDTO response = approvalService.createRequest(requestDTO);

        // Assert
        assertNotNull(response);
        assertEquals(ApprovalStatus.PENDING, response.getStatus());
        assertEquals(ApprovalType.NORMAL, response.getApprovalType());
        assertNotNull(response.getStartDate());
        assertNotNull(response.getStartTime());

        verify(approvalRequestRepository).save(any(ApprovalRequest.class));
    }

    @Test
    void createRequest_InvalidNormal_ThrowsException() {
        // Arrange
        ApprovalRequestDTO requestDTO = new ApprovalRequestDTO();
        requestDTO.setRequestedById(1);
        requestDTO.setBusId(101);
        requestDTO.setApprovalType(ApprovalType.NORMAL);
        requestDTO.setStartDate(null); // Invalid: missing date/time fields

        when(userRepository.findById(1)).thenReturn(Optional.of(testDriver));
        when(busRepository.findById(101)).thenReturn(Optional.of(testBus));

        // Act & Assert
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            approvalService.createRequest(requestDTO);
        });

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("Start date, end date, start time, and end time are required for normal approvals", ex.getReason());
        verify(approvalRequestRepository, never()).save(any(ApprovalRequest.class));
    }

    @Test
    void updateStatus_Approved_Success() {
        // Arrange
        when(approvalRequestRepository.findById(10)).thenReturn(Optional.of(testEmergencyRequest));
        when(approvalRequestRepository.save(any(ApprovalRequest.class))).thenAnswer(invocation -> {
            ApprovalRequest req = invocation.getArgument(0);
            req.setStatus(ApprovalStatus.APPROVED);
            req.setReason("Looks good");
            return req;
        });

        // Act
        ApprovalResponseDTO response = approvalService.updateStatus(10, ApprovalStatus.APPROVED, "Looks good");

        // Assert
        assertNotNull(response);
        assertEquals(ApprovalStatus.APPROVED, response.getStatus());
        assertEquals("Looks good", response.getReason());

        verify(notificationService).createNotification(
                eq("Approval Request APPROVED"),
                contains("Your approval request for Bus WP-1234 has been approved. Reason: Looks good"),
                eq(NotificationCategory.USER_UPDATE),
                eq("SYSTEM"),
                eq(List.of(1))
        );

        verify(scheduleAssignmentService).rescheduleEmergency(
                eq(testEmergencyRequest.getBus()),
                any(LocalDate.class)
        );
    }

    @Test
    void updateStatus_ApprovedNormal_Success() {
        // Arrange
        when(approvalRequestRepository.findById(20)).thenReturn(Optional.of(testNormalRequest));
        when(approvalRequestRepository.save(any(ApprovalRequest.class))).thenAnswer(invocation -> {
            ApprovalRequest req = invocation.getArgument(0);
            req.setStatus(ApprovalStatus.APPROVED);
            req.setReason("Approved");
            return req;
        });

        // Act
        ApprovalResponseDTO response = approvalService.updateStatus(20, ApprovalStatus.APPROVED, "Approved");

        // Assert
        assertNotNull(response);
        assertEquals(ApprovalStatus.APPROVED, response.getStatus());

        verify(scheduleAssignmentService).rescheduleNormal(
                eq(testNormalRequest.getBus()),
                eq(testNormalRequest.getStartDate()),
                eq(testNormalRequest.getEndDate()),
                eq(testNormalRequest.getStartTime()),
                eq(testNormalRequest.getEndTime())
        );
    }

    @Test
    void updateStatus_InvalidStatus_ThrowsException() {
        // Act & Assert
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            approvalService.updateStatus(10, ApprovalStatus.PENDING, "Not allowed");
        });

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("Admins can only update status to APPROVED or REJECTED", ex.getReason());
    }
}
