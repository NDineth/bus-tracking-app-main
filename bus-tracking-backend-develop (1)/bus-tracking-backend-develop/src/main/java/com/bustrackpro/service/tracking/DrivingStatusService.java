package com.bustrackpro.service.tracking;

import com.bustrackpro.dto.request.tracking.DrivingAlertRequest;
import com.bustrackpro.dto.request.tracking.DrivingStatusRequest;
import com.bustrackpro.modal.tracking.DrivingBehaviorType;
import com.bustrackpro.repository.bus.BusRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service to persist driving status events from the IoT Hub
 * through DrivingBehaviorService, which persists each event once and handles alerting.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DrivingStatusService {

    private final BusRepository busRepository;
    private final DrivingBehaviorService drivingBehaviorService;

    /**
     * Process and persist a driving status event received from IoT Hub.
     * Also forwards non-NORMAL statuses to the alert pipeline.
     */
    @Transactional
    public void processDrivingStatus(DrivingStatusRequest request) {

        busRepository.findById(request.getBusId())
                .orElseThrow(() -> new RuntimeException(
                        "Bus not found with id: " + request.getBusId()));

        DrivingBehaviorType behaviorType;
        try {
            behaviorType = DrivingBehaviorType.valueOf(request.getStatus());
        } catch (IllegalArgumentException e) {
            log.warn("Unknown driving status '{}' for bus {}. Defaulting to NORMAL.",
                    request.getStatus(), request.getBusId());
            behaviorType = DrivingBehaviorType.NORMAL;
        }

        // The alert pipeline persists each event once, including NORMAL statuses.
        DrivingAlertRequest alert = DrivingAlertRequest.builder()
                .busId(request.getBusId())
                .behavior(behaviorType)
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .timestamp(request.getRecordedAt() == null ? null
                        : request.getRecordedAt().toInstant().toEpochMilli())
                .build();
        drivingBehaviorService.processDrivingAlert(alert);
    }
}
