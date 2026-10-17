package com.bustrackpro.service.tracking;

import com.bustrackpro.dto.request.tracking.DrivingAlertRequest;
import com.bustrackpro.dto.request.tracking.DrivingStatusRequest;
import com.bustrackpro.modal.bus.Bus;
import com.bustrackpro.modal.tracking.DrivingBehaviorType;
import com.bustrackpro.modal.tracking.DrivingStatusLog;
import com.bustrackpro.repository.bus.BusRepository;
import com.bustrackpro.repository.tracking.DrivingStatusLogRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service to persist driving status events from the IoT Hub
 * and forward non-NORMAL statuses to the DrivingBehaviorService for alerting.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DrivingStatusService {

    private final DrivingStatusLogRepository statusLogRepository;
    private final BusRepository busRepository;
    private final DrivingBehaviorService drivingBehaviorService;

    /**
     * Process and persist a driving status event received from IoT Hub.
     * Also forwards non-NORMAL statuses to the alert pipeline.
     */
    @Transactional
    public void processDrivingStatus(DrivingStatusRequest request) {

        Bus bus = busRepository.findById(request.getBusId())
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

        LocalDateTime recordedAt = request.getRecordedAt() != null
                ? request.getRecordedAt().toLocalDateTime()
                : LocalDateTime.now();

        // Persist the status log
        DrivingStatusLog statusLog = new DrivingStatusLog();
        statusLog.setBus(bus);
        statusLog.setStatus(behaviorType);
        statusLog.setLatitude(request.getLatitude());
        statusLog.setLongitude(request.getLongitude());
        statusLog.setRecordedAt(recordedAt);
        statusLogRepository.save(statusLog);

        log.info("Saved driving status [{}] for bus {} at {}",
                behaviorType, request.getBusId(), recordedAt);

        // Forward non-NORMAL statuses to the driving behavior alert pipeline
        if (behaviorType != DrivingBehaviorType.NORMAL) {
            DrivingAlertRequest alert = DrivingAlertRequest.builder()
                    .busId(request.getBusId())
                    .behavior(behaviorType)
                    .timestamp(recordedAt.atZone(java.time.ZoneId.systemDefault())
                            .toInstant().toEpochMilli())
                    .build();
            drivingBehaviorService.processDrivingAlert(alert);
        }
    }
}
