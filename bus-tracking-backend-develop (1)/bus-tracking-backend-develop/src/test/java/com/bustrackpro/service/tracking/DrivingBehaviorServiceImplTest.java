package com.bustrackpro.service.tracking;

import com.bustrackpro.dto.request.tracking.AccelerationReadingDto;
import com.bustrackpro.dto.request.tracking.DrivingAlertRequest;
import com.bustrackpro.modal.Notification;
import com.bustrackpro.modal.NotificationCategory;
import com.bustrackpro.modal.bus.Bus;
import com.bustrackpro.modal.tracking.AlertPolicy;
import com.bustrackpro.modal.tracking.DrivingBehaviorType;
import com.bustrackpro.modal.user.Users;
import com.bustrackpro.repository.bus.BusRepository;
import com.bustrackpro.repository.tracking.DrivingStatusLogRepository;
import com.bustrackpro.repository.user.UserRepository;
import com.bustrackpro.service.notification.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Collections;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DrivingBehaviorServiceImplTest {
    @Mock NotificationService notifications;
    @Mock BusRepository buses;
    @Mock UserRepository users;
    @Mock DrivingStatusLogRepository logs;
    @Mock AlertPolicyService policies;
    @InjectMocks DrivingBehaviorServiceImpl service;
    private AlertPolicy policy;

    @BeforeEach
    void setup() {
        Bus bus = new Bus();
        bus.setId(1); bus.setBusNumber("QA-1001");
        Users owner = new Users(); owner.setId(9); bus.setOwner(owner);
        when(buses.findById(1)).thenReturn(Optional.of(bus));
        policy = new AlertPolicy();
        when(policies.getPolicy()).thenReturn(policy);
    }

    private AccelerationReadingDto brake() {
        AccelerationReadingDto reading = new AccelerationReadingDto();
        reading.setX(-4.0); reading.setY(0.0);
        return reading;
    }

    @Test
    void lowRiskBatchIsLoggedWithoutNotification() {
        service.analyzeDrivingBehavior(1, Collections.nCopies(1, brake()), 6.9, 79.8);
        verify(logs).save(any());
        verifyNoInteractions(notifications);
    }

    @Test
    void highRiskBatchIncludesStructuredScoreAndSeverity() {
        Notification notification = new Notification();
        when(notifications.createNotification(anyString(), anyString(), eq(NotificationCategory.SYSTEM_ALERT), anyString(), anyList())).thenReturn(notification);
        service.analyzeDrivingBehavior(1, Collections.nCopies(7, brake()), 6.9, 79.8);
        assertEquals(65, notification.getSafetyScore());
        assertEquals("HIGH", notification.getSeverity());
    }

    @Test
    void exactCautionBoundaryDoesNotNotify() {
        service.analyzeDrivingBehavior(1, Collections.nCopies(2, brake()), 6.9, 79.8);
        verifyNoInteractions(notifications);
    }

    @Test
    void configuredCountsRepeatAndCrashIsImmediate() {
        policy.setHighSpeedEvents(2);
        when(notifications.createNotification(anyString(), anyString(), any(), anyString(), anyList())).thenAnswer(invocation -> new Notification());
        for (int i = 0; i < 4; i++) service.processDrivingAlert(DrivingAlertRequest.builder().busId(1).behavior(DrivingBehaviorType.HIGH_SPEED).build());
        verify(notifications, times(2)).createNotification(anyString(), anyString(), any(), anyString(), anyList());
        service.processDrivingAlert(DrivingAlertRequest.builder().busId(1).behavior(DrivingBehaviorType.CRASH_DETECTED).build());
        verify(notifications, times(3)).createNotification(anyString(), anyString(), any(), anyString(), anyList());
        verify(logs, times(5)).save(any());
    }
}
