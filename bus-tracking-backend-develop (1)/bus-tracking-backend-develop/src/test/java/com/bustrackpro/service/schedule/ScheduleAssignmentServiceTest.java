package com.bustrackpro.service.schedule;

import com.bustrackpro.modal.bus.Bus;
import com.bustrackpro.modal.bus.BusDriver;
import com.bustrackpro.modal.schedule.*;
import com.bustrackpro.repository.schedule.ScheduleAssignmentRepository;
import com.bustrackpro.repository.schedule.TripRepository;
import com.bustrackpro.repository.schedule.TimetableRepository;
import com.bustrackpro.repository.bus.BusRepository;
import com.bustrackpro.repository.schedule.BusDriverRepository;
import com.bustrackpro.repository.finance.DelayPenaltyRuleRepository;
import com.bustrackpro.repository.finance.FinanceRateCardRepository;
import com.bustrackpro.repository.tracking.GpsLogRepository;
import com.bustrackpro.repository.tracking.RouteViolationRepository;
import com.bustrackpro.repository.BusRouteHaltRepository;
import com.bustrackpro.service.notification.NotificationService;
import com.bustrackpro.repository.user.UserRepository;
import com.bustrackpro.repository.finance.PenaltyRecordRepository;
import com.bustrackpro.modal.finance.PenaltyRecord;
import com.bustrackpro.modal.user.Users;
import com.bustrackpro.modal.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ScheduleAssignmentServiceTest {

    @Mock
    private ScheduleAssignmentRepository assignmentRepository;

    @Mock
    private TripRepository tripRepository;

    @Mock
    private TimetableRepository timetableRepository;

    @Mock
    private BusRepository busRepository;

    @Mock
    private BusDriverRepository busDriverRepository;

    @Mock
    private DelayPenaltyRuleRepository delayPenaltyRuleRepository;

    @Mock
    private FinanceRateCardRepository financeRateCardRepository;

    @Mock
    private GpsLogRepository gpsLogRepository;

    @Mock
    private RouteViolationRepository routeViolationRepository;

    @Mock
    private BusRouteHaltRepository busRouteHaltRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PenaltyRecordRepository penaltyRecordRepository;

    @InjectMocks
    private ScheduleAssignmentService assignmentService;

    private Trip testTrip;
    private ScheduleAssignment testAssignment;

    @BeforeEach
    void setUp() {
        testTrip = new Trip();
        testTrip.setId(1);
        testTrip.setDepartureTime(LocalTime.now().minusMinutes(10)); // 10 minutes delay by default
        testTrip.setArrivalTime(LocalTime.now().plusHours(1));
        testTrip.setStatus(TripStatus.ACTIVE);

        testAssignment = new ScheduleAssignment();
        testAssignment.setId(100);
        testAssignment.setTrip(testTrip);
        testAssignment.setAssignmentDate(LocalDate.now());
        testAssignment.setStatus(AssignmentStatus.SCHEDULED);
        
        Bus testBus = new Bus();
        testBus.setId(5);
        testAssignment.setBus(testBus);
    }

    @Test
    void testStartAssignment_Success_UpdatesTripStatusToInProgress() {
        when(assignmentRepository.findById(100)).thenReturn(Optional.of(testAssignment));
        when(delayPenaltyRuleRepository.findAllByOrderByMinDelayMinutesDesc()).thenReturn(Collections.emptyList());
        when(assignmentRepository.save(any(ScheduleAssignment.class))).thenReturn(testAssignment);

        assignmentService.startAssignment(100);

        assertEquals(TripStatus.IN_PROGRESS, testTrip.getStatus());
        verify(tripRepository, times(1)).save(testTrip);
        assertEquals(AssignmentStatus.IN_PROGRESS, testAssignment.getStatus());
    }

    @Test
    void testStartAssignment_TurnLoss_UpdatesTripStatusToCancelled() {
        // departure is now 35 minutes in the past -> >30 min delay -> TURN_LOSS
        testTrip.setDepartureTime(LocalTime.now().minusMinutes(35));

        when(assignmentRepository.findById(100)).thenReturn(Optional.of(testAssignment));
        when(assignmentRepository.save(any(ScheduleAssignment.class))).thenReturn(testAssignment);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(new Users()));
        when(assignmentRepository.countByBusIdAndStatus(any(), any())).thenReturn(1L);

        assignmentService.startAssignment(100);

        assertEquals(TripStatus.CANCELLED, testTrip.getStatus());
        verify(tripRepository, times(1)).save(testTrip);
        assertEquals(AssignmentStatus.TURN_LOSS, testAssignment.getStatus());
    }

    @Test
    void testAutoDetectUnstartedBuses() {
        // Create an unstarted assignment that is 40 minutes late
        ScheduleAssignment lateSa = new ScheduleAssignment();
        lateSa.setId(101);
        lateSa.setAssignmentDate(LocalDate.now());
        lateSa.setStatus(AssignmentStatus.SCHEDULED);
        
        Trip lateTrip = new Trip();
        lateTrip.setId(2);
        lateTrip.setDepartureTime(LocalTime.now().minusMinutes(40));
        lateSa.setTrip(lateTrip);

        Bus lateBus = new Bus();
        lateBus.setId(10);
        lateBus.setBusNumber("WP-NC-1234");
        lateSa.setBus(lateBus);

        when(assignmentRepository.findByAssignmentDate(any(LocalDate.class))).thenReturn(new java.util.ArrayList<>(Collections.singletonList(lateSa)));
        when(assignmentRepository.save(any(ScheduleAssignment.class))).thenReturn(lateSa);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(new Users()));
        when(assignmentRepository.countByBusIdAndStatus(any(), any())).thenReturn(1L);

        assignmentService.autoDetectUnstartedBuses();

        assertEquals(AssignmentStatus.TURN_LOSS, lateSa.getStatus());
        assertEquals(TripStatus.CANCELLED, lateTrip.getStatus());
        verify(assignmentRepository, atLeastOnce()).save(lateSa);
    }

    @Test
    void testHandleTurnLoss_TriggersSuspensionAndRescheduling() {
        // Create an unstarted assignment
        ScheduleAssignment sa = new ScheduleAssignment();
        sa.setId(102);
        sa.setAssignmentDate(LocalDate.now());
        sa.setStatus(AssignmentStatus.SCHEDULED);
        
        Trip trip = new Trip();
        trip.setId(3);
        trip.setDepartureTime(LocalTime.now().minusMinutes(40));
        trip.setArrivalTime(LocalTime.now().plusMinutes(20));
        sa.setTrip(trip);

        Bus bus = new Bus();
        bus.setId(11);
        bus.setBusNumber("WP-NC-5555");
        sa.setBus(bus);

        // When counting faults, return 3 to trigger the 3-fault suspension rule
        when(assignmentRepository.countByBusIdAndStatus(any(), eq(AssignmentStatus.TURN_LOSS))).thenReturn(3L);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(new Users()));
        when(assignmentRepository.save(any(ScheduleAssignment.class))).thenReturn(sa);

        // Mock next 5 scheduled assignments to reschedule
        ScheduleAssignment nextSa = new ScheduleAssignment();
        nextSa.setId(103);
        nextSa.setAssignmentDate(LocalDate.now());
        nextSa.setStatus(AssignmentStatus.SCHEDULED);
        nextSa.setBus(bus);
        
        Trip nextTrip = new Trip();
        nextTrip.setId(4);
        nextTrip.setDepartureTime(LocalTime.now().plusHours(2));
        nextTrip.setArrivalTime(LocalTime.now().plusHours(3));
        nextSa.setTrip(nextTrip);

        when(assignmentRepository.findNextScheduledAssignments(any(), any(), any()))
                .thenReturn(Collections.singletonList(nextSa));

        // Mock replacement resources
        Bus replacementBus = new Bus();
        replacementBus.setId(12);
        replacementBus.setBusNumber("WP-NC-9999");
        when(busRepository.findAll()).thenReturn(Collections.singletonList(replacementBus));

        BusDriver replacementDriver = new BusDriver();
        replacementDriver.setId(20);
        Users driverUser = new Users();
        driverUser.setFirstName("John");
        replacementDriver.setUsers(driverUser);
        when(busDriverRepository.findAll()).thenReturn(Collections.singletonList(replacementDriver));

        // Mock no conflicts for replacement resources
        when(assignmentRepository.countBusConflicts(eq(12), any(), any(), any())).thenReturn(0L);
        when(assignmentRepository.countDriverConflicts(eq(20), any(), any(), any())).thenReturn(0L);

        assignmentService.handleTurnLoss(sa);

        // Verify nextSa was rescheduled to replacementBus and replacementDriver
        assertEquals(replacementBus, nextSa.getBus());
        assertEquals(replacementDriver, nextSa.getDriver());
        assertEquals(AssignmentStatus.SCHEDULED, nextSa.getStatus());
        verify(assignmentRepository, atLeastOnce()).save(nextSa);
    }

    @Test
    void testCompleteAssignment_UpdatesTripStatusToCompleted() {
        when(assignmentRepository.findById(100)).thenReturn(Optional.of(testAssignment));
        when(gpsLogRepository.findByScheduleAssignmentOrderByRecordedAtAsc(any())).thenReturn(Collections.emptyList());
        when(financeRateCardRepository.findTopByOrderByCreatedAtDesc()).thenReturn(Optional.empty());
        when(assignmentRepository.save(any(ScheduleAssignment.class))).thenReturn(testAssignment);

        assignmentService.completeAssignment(100);

        assertEquals(TripStatus.COMPLETED, testTrip.getStatus());
        verify(tripRepository, times(1)).save(testTrip);
        assertEquals(AssignmentStatus.COMPLETED, testAssignment.getStatus());
    }
}
