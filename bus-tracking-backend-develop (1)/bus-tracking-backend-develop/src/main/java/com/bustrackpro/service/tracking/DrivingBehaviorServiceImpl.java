package com.bustrackpro.service.tracking;

import com.bustrackpro.dto.request.tracking.DrivingAlertRequest;
import com.bustrackpro.dto.request.tracking.AccelerationReadingDto;
import com.bustrackpro.modal.NotificationCategory;
import com.bustrackpro.modal.bus.Bus;
import com.bustrackpro.modal.tracking.DrivingBehaviorType;
import com.bustrackpro.modal.tracking.DrivingStatusLog;
import com.bustrackpro.modal.user.Users;
import com.bustrackpro.repository.bus.BusRepository;
import com.bustrackpro.repository.tracking.DrivingStatusLogRepository;
import com.bustrackpro.repository.user.UserRepository;
import com.bustrackpro.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DrivingBehaviorServiceImpl implements DrivingBehaviorService {

    private final NotificationService notificationService;
    private final BusRepository busRepository;
    private final UserRepository userRepository;
    private final DrivingStatusLogRepository drivingStatusLogRepository;

    // Stateful tracking: BusId -> (BehaviorType -> Frequency)
    private final Map<Integer, Map<DrivingBehaviorType, Integer>> eventCounters = new ConcurrentHashMap<>();

    // Thresholds for warning
    private static final Map<DrivingBehaviorType, Integer> WARNING_THRESHOLDS = Map.of(
            DrivingBehaviorType.CRASH_DETECTED, 1,
            DrivingBehaviorType.HIGH_SPEED, 3,
            DrivingBehaviorType.SUDDEN_BRAKING, 2,
            DrivingBehaviorType.SUDDEN_ACCELERATION, 2,
            DrivingBehaviorType.SUDDEN_TURNING, 2
    );

    @Override
    @Transactional
    public void processDrivingAlert(DrivingAlertRequest alert) {
        DrivingBehaviorType behavior = alert.getBehavior();

        // Persist the driving status log (all statuses including NORMAL)
         saveDrivingStatusLog(alert);

        if (Objects.isNull(behavior) || behavior == DrivingBehaviorType.NORMAL) {
            return;
        }

        // Update frequency counter
        int count = incrementCounter(alert.getBusId(), behavior);
        int threshold = WARNING_THRESHOLDS.getOrDefault(behavior, Integer.MAX_VALUE);

        log.info("Received alert {} for Bus ID: {}. Current frequency: {}/{}", behavior, alert.getBusId(), count, threshold);

        if (count >= threshold) {
            log.warn("Threshold reached for {}. Triggering notification for Bus ID: {}", behavior, alert.getBusId());
            triggerWarning(alert.getBusId(), behavior);
            // Reset counter after warning to allow for future alerts
            resetCounter(alert.getBusId(), behavior);
        }
    }

    private int incrementCounter(Integer busId, DrivingBehaviorType behavior) {
        Map<DrivingBehaviorType, Integer> busCounters = eventCounters.computeIfAbsent(busId, k -> new ConcurrentHashMap<>());
        return busCounters.merge(behavior, 1, Integer::sum);
    }

    private void resetCounter(Integer busId, DrivingBehaviorType behavior) {
        Map<DrivingBehaviorType, Integer> busCounters = eventCounters.get(busId);
        if (busCounters != null) {
            busCounters.remove(behavior);
        }
    }

    private void triggerWarning(Integer busId, DrivingBehaviorType behavior) {
        Bus bus = busRepository.findById(busId)
                .orElse(null);

        if (bus == null) {
            log.error("Bus not found for ID: {}", busId);
            return;
        }

        String title = "Driving Alert: " + behavior.name().replace("_", " ");
        String body = getWarningMessage(behavior, bus.getBusNumber());
        
        java.util.Set<Integer> receiverIds = new java.util.HashSet<>();
        
        // 1. Notify Driver assigned to this bus
        if (bus.getDriver() != null) {
            receiverIds.add(bus.getDriver().getId());
        }

        // 2. Notify Bus Owner
        if (bus.getOwner() != null) {
            receiverIds.add(bus.getOwner().getId());
        }

        if (receiverIds.isEmpty()) {
            log.info("No recipients (owner or driver) found for Bus ID: {}. Skipping notification.", busId);
            return;
        }

        notificationService.createNotification(
                title,
                body,
                NotificationCategory.SYSTEM_ALERT,
                "DRIVING_MONITOR",
                new java.util.ArrayList<>(receiverIds)
        );
    }

    private String getWarningMessage(DrivingBehaviorType behavior, String busNumber) {
        switch (behavior) {
            case HIGH_SPEED:
                return "Safety Warning: Bus " + busNumber + " has consistently exceeded the speed limit. Please ensure safe driving standards.";
            case SUDDEN_BRAKING:
                return "Caution: Frequent sudden braking detected for Bus " + busNumber + ". This may indicate unsafe road conditions or aggressive driving.";
            case SUDDEN_ACCELERATION:
                return "Alert: Aggressive acceleration patterns detected for Bus " + busNumber + ". Please monitor driving behavior.";
            case SUDDEN_TURNING:
                return "Warning: Sharp or unsafe turning maneuvers detected for Bus " + busNumber + ".";
            case CRASH_DETECTED:
                return "URGENT: A potential crash has been detected for Bus " + busNumber + "! Please check on the driver and passengers immediately.";
            default:
                return "A persistent driving behavior alert has been triggered for Bus " + busNumber + ".";
        }
    }

    /**
     * Persist a driving status log entry for every status event received.
     */
    private void saveDrivingStatusLog(DrivingAlertRequest alert) {
        Bus bus = busRepository.findById(alert.getBusId()).orElse(null);
        if (bus == null) {
            log.warn("Cannot save driving status log — Bus not found for ID: {}", alert.getBusId());
            return;
        }

        DrivingStatusLog statusLog = new DrivingStatusLog();
        statusLog.setBus(bus);
        statusLog.setStatus(alert.getBehavior() != null ? alert.getBehavior() : DrivingBehaviorType.NORMAL);
        statusLog.setLatitude(alert.getLatitude());
        statusLog.setLongitude(alert.getLongitude());
        statusLog.setRecordedAt(java.time.LocalDateTime.now());
        drivingStatusLogRepository.save(statusLog);

        log.info("Saved driving status log [{}] for bus {}", statusLog.getStatus(), alert.getBusId());
    }

    @Override
    @Transactional
    public void analyzeDrivingBehavior(Integer busId, List<AccelerationReadingDto> acceleration, Double latitude, Double longitude) {
        Bus bus = busRepository.findById(busId).orElse(null);
        if (bus == null) {
            log.warn("Cannot analyze driving behavior — Bus not found for ID: {}", busId);
            return;
        }

        int harshBrakeCount = 0;
        int aggressiveAccelCount = 0;
        int dangerousTurnCount = 0;

        for (AccelerationReadingDto acc : acceleration) {
            if (Objects.nonNull(acc.getX()) && acc.getX() < -3.0) {
                harshBrakeCount++;
            }
            if (Objects.nonNull(acc.getX()) && acc.getX() > 3.0) {
                aggressiveAccelCount++;
            }
            if (Objects.nonNull(acc.getY()) && Math.abs(acc.getY()) > 2.5) {
                dangerousTurnCount++;
            }
        }

        // If any harsh events were detected, save logs and trigger safety score notification
        if (harshBrakeCount > 0 || aggressiveAccelCount > 0 || dangerousTurnCount > 0) {
            int penalty = (harshBrakeCount * 5) + (aggressiveAccelCount * 4) + (dangerousTurnCount * 3);
            int safetyScore = Math.max(0, 100 - penalty);

            String riskLevel;
            if (safetyScore >= 90) {
                riskLevel = "Low Risk";
            } else if (safetyScore >= 70) {
                riskLevel = "Moderate Risk";
            } else {
                riskLevel = "High Risk";
            }

            // Save individual driving status logs for occurred violations
            if (harshBrakeCount > 0) {
                saveDrivingStatusLog(bus, DrivingBehaviorType.SUDDEN_BRAKING, latitude, longitude);
            }
            if (aggressiveAccelCount > 0) {
                saveDrivingStatusLog(bus, DrivingBehaviorType.SUDDEN_ACCELERATION, latitude, longitude);
            }
            if (dangerousTurnCount > 0) {
                saveDrivingStatusLog(bus, DrivingBehaviorType.SUDDEN_TURNING, latitude, longitude);
            }

            String title = "Driving Safety Alert: Bus " + bus.getBusNumber();
            String body = "Harsh Braking: " + harshBrakeCount + "\n" +
                          "Aggressive Acceleration: " + aggressiveAccelCount + "\n" +
                          "Dangerous Turns: " + dangerousTurnCount + "\n" +
                          "Safety Score: " + safetyScore + " / 100\n" +
                          "Driver Rating: " + riskLevel;

            Set<Integer> receiverIds = new HashSet<>();
            if (bus.getDriver() != null) {
                receiverIds.add(bus.getDriver().getId());
            }
            if (bus.getOwner() != null) {
                receiverIds.add(bus.getOwner().getId());
            }

            if (!receiverIds.isEmpty()) {
                notificationService.createNotification(
                        title,
                        body,
                        NotificationCategory.SYSTEM_ALERT,
                        "DRIVING_MONITOR",
                        new java.util.ArrayList<>(receiverIds)
                );
                log.info("Sent driving safety score notification for Bus ID: {}. Safety Score: {}/100 ({})", 
                        busId, safetyScore, riskLevel);
            }
        } else {
            // Log normal behavior
            saveDrivingStatusLog(bus, DrivingBehaviorType.NORMAL, latitude, longitude);
        }
    }

    private void saveDrivingStatusLog(Bus bus, DrivingBehaviorType behavior, Double latitude, Double longitude) {
        DrivingStatusLog statusLog = new DrivingStatusLog();
        statusLog.setBus(bus);
        statusLog.setStatus(behavior);
        statusLog.setLatitude(latitude);
        statusLog.setLongitude(longitude);
        statusLog.setRecordedAt(java.time.LocalDateTime.now());
        drivingStatusLogRepository.save(statusLog);
        log.info("Saved driving status log [{}] for bus {}", behavior, bus.getId());
    }
}
