package com.bustrackpro.service.tracking;

import com.bustrackpro.dto.request.tracking.GpsUpdateRequest;
import com.bustrackpro.dto.response.tracking.*;
import com.bustrackpro.modal.bus.Bus;
import com.bustrackpro.modal.route.BusRouteHalt;
import com.bustrackpro.modal.schedule.AssignmentStatus;
import com.bustrackpro.modal.schedule.ScheduleAssignment;
import com.bustrackpro.modal.tracking.AccelerationData;
import com.bustrackpro.modal.tracking.GpsLog;
import com.bustrackpro.modal.tracking.RouteViolation;
import com.bustrackpro.modal.tracking.ViolationType;
import com.bustrackpro.modal.approval.ApprovalRequest;
import com.bustrackpro.modal.approval.ApprovalStatus;
import com.bustrackpro.modal.approval.ApprovalType;
import com.bustrackpro.repository.approval.ApprovalRequestRepository;
import com.bustrackpro.repository.BusRouteHaltRepository;
import com.bustrackpro.repository.bus.BusRepository;
import com.bustrackpro.repository.schedule.ScheduleAssignmentRepository;
import com.bustrackpro.repository.tracking.GpsLogRepository;
import com.bustrackpro.repository.tracking.RouteViolationRepository;
import com.bustrackpro.service.notification.NotificationService;
import com.bustrackpro.util.ComplianceResult;
import com.bustrackpro.util.RouteComplianceUtil;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of the GPS tracking and route compliance service.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TrackingServiceImpl implements TrackingService {

    private final BusRepository busRepository;
    private final ScheduleAssignmentRepository assignmentRepository;
    private final BusRouteHaltRepository routeHaltRepository;
    private final GpsLogRepository gpsLogRepository;
    private final RouteViolationRepository violationRepository;
    private final DrivingBehaviorService drivingBehaviorService;
    private final NotificationService notificationService;
    private final ApprovalRequestRepository approvalRequestRepository;

    @Override
    @Transactional
    public TrackingResponse processGpsUpdate(GpsUpdateRequest request) {

        Bus bus = busRepository.findByDeviceReferenceNumber(request.getDeviceReferenceNumber())
                .orElseThrow(() -> new RuntimeException(
                        "Bus not found with deviceReferenceNumber: " + request.getDeviceReferenceNumber()));

        LocalDateTime recordedAt = request.getRecordedAt() != null
                ? request.getRecordedAt().toLocalDateTime()
                : LocalDateTime.now();

        if (request.getLatitude() != null && request.getLatitude() == 0.0
                && request.getLongitude() != null && request.getLongitude() == 0.0
                && request.getSpeed() != null && request.getSpeed() == -100.0) {
            log.info("Ignoring dummy GPS telemetry update (latitude=0, longitude=0, speed=-100) for deviceReferenceNumber: {}", request.getDeviceReferenceNumber());
            return TrackingResponse.builder()
                    .busId(bus.getId())
                    .onRoute(null)
                    .timestamp(recordedAt.toString())
                    .build();
        }

        ScheduleAssignment activeAssignment =
                findActiveAssignment(bus, recordedAt);

        if (activeAssignment == null) {
            saveGpsLog(bus, null, request, recordedAt);
            if (request.getAcceleration() != null && !request.getAcceleration().isEmpty()) {
                drivingBehaviorService.analyzeDrivingBehavior(bus.getId(), request.getAcceleration(), request.getLatitude(), request.getLongitude());
            }
            return TrackingResponse.builder()
                    .busId(bus.getId())
                    .onRoute(null)
                    .timestamp(recordedAt.toString())
                    .build();
        }

        var busRoute = activeAssignment.getTrip()
                .getTimetable().getBusRoute();

        List<BusRouteHalt> routeHalts = routeHaltRepository
                .findByBusRouteOrderByStopOrderAsc(busRoute);

        List<GpsLog> previousLogs = gpsLogRepository
                .findByScheduleAssignmentOrderByRecordedAtAsc(
                        activeAssignment);

        ComplianceResult compliance = runComplianceAnalysis(
                request.getLatitude(), request.getLongitude(),
                routeHalts, previousLogs);

        saveGpsLog(bus, activeAssignment, request, recordedAt);

        if (request.getAcceleration() != null && !request.getAcceleration().isEmpty()) {
            drivingBehaviorService.analyzeDrivingBehavior(bus.getId(), request.getAcceleration(), request.getLatitude(), request.getLongitude());
        }

        List<ViolationDetail> violationDetails = recordViolations(
                bus, activeAssignment, compliance,
                request.getLatitude(), request.getLongitude(),
                recordedAt);

        return TrackingResponse.builder()
                .busId(bus.getId())
                .routeNumber(busRoute.getRouteNumber())
                .tripCode(activeAssignment.getTrip().getTripCode())
                .onRoute(!compliance.isRouteDeviation())
                .deviationMeters(compliance.getMinDistanceToRouteMeters())
                .nearestHaltName(compliance.getNearestHaltName())
                .skippedHalts(compliance.getSkippedHaltNames())
                .violations(violationDetails)
                .timestamp(recordedAt.toString())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActiveBusLocationResponse> getActiveBusLocations() {
        LocalDateTime thirtyMinutesAgo = LocalDateTime.now().minusMinutes(30);
        List<GpsLog> latestLogs = gpsLogRepository.findLatestGpsLogs(thirtyMinutesAgo);

        return latestLogs.stream().map(log -> {
            Long routeId = null;
            String routeNumber = null;
            if (log.getScheduleAssignment() != null && log.getScheduleAssignment().getTrip() != null
                && log.getScheduleAssignment().getTrip().getTimetable() != null
                && log.getScheduleAssignment().getTrip().getTimetable().getBusRoute() != null) {
                var route = log.getScheduleAssignment().getTrip().getTimetable().getBusRoute();
                routeId = route.getId();
                routeNumber = route.getRouteNumber();
            }
            
            return ActiveBusLocationResponse.builder()
                    .busId(log.getBus().getId())
                    .registrationNumber(log.getBus().getBusNumber())
                    .latitude(log.getLatitude())
                    .longitude(log.getLongitude())
                    .speed(log.getSpeed())
                    .recordedAt(log.getRecordedAt())
                    .routeId(routeId)
                    .routeNumber(routeNumber)
                    .build();
        }).collect(Collectors.toList());
    }

    // ------------------------------------------------------------------ //
    //  Helper methods                                                     //
    // ------------------------------------------------------------------ //

    /**
     * Find the currently active assignment for a bus.
     * Looks for SCHEDULED or IN_PROGRESS assignments on today's date
     * whose trip time window covers the current time (with buffer).
     */
    private ScheduleAssignment findActiveAssignment(
            Bus bus, LocalDateTime now) {

        LocalDate today = now.toLocalDate();

        List<AssignmentStatus> activeStatuses = List.of(
                AssignmentStatus.IN_PROGRESS);

        List<ScheduleAssignment> assignments = assignmentRepository
                .findByBusIdAndDateAndStatusIn(
                        bus.getId(), today, activeStatuses);

        if (!assignments.isEmpty()) {
            return assignments.get(0);
        }

        return null;
    }

    /**
     * Run the compliance algorithm by extracting halt coordinates
     * and previous log coordinates into parallel lists.
     */
    private ComplianceResult runComplianceAnalysis(
            double currentLat, double currentLng,
            List<BusRouteHalt> routeHalts,
            List<GpsLog> previousLogs) {

        List<Double> haltLats = new ArrayList<>();
        List<Double> haltLngs = new ArrayList<>();
        List<String> haltNames = new ArrayList<>();
        List<Integer> haltOrders = new ArrayList<>();

        for (BusRouteHalt rh : routeHalts) {
            haltLats.add(rh.getBusHalt().getLatitude());
            haltLngs.add(rh.getBusHalt().getLongitude());
            haltNames.add(rh.getBusHalt().getName());
            haltOrders.add(rh.getStopOrder());
        }

        List<Double> logLats = previousLogs.stream()
                .map(GpsLog::getLatitude)
                .collect(Collectors.toList());
        List<Double> logLngs = previousLogs.stream()
                .map(GpsLog::getLongitude)
                .collect(Collectors.toList());

        return RouteComplianceUtil.analyseCompliance(
                currentLat, currentLng,
                haltLats, haltLngs, haltNames, haltOrders,
                logLats, logLngs);
    }

    /**
     * Persist a GPS log entry.
     */
    private void saveGpsLog(Bus bus, ScheduleAssignment assignment,
                            GpsUpdateRequest request,
                            LocalDateTime recordedAt) {
        GpsLog gpsLog = new GpsLog();
        gpsLog.setBus(bus);
        gpsLog.setScheduleAssignment(assignment);
        gpsLog.setLatitude(request.getLatitude());
        gpsLog.setLongitude(request.getLongitude());
        gpsLog.setSpeed(request.getSpeed());
        gpsLog.setRecordedAt(recordedAt);

        if (request.getAcceleration() != null && !request.getAcceleration().isEmpty()) {
            List<AccelerationData> accEntities = request.getAcceleration().stream()
                    .map(dto -> AccelerationData.builder()
                            .gpsLog(gpsLog)
                            .x(dto.getX())
                            .y(dto.getY())
                            .z(dto.getZ())
                            .build())
                    .collect(Collectors.toList());
            gpsLog.setAcceleration(accEntities);
        }

        gpsLogRepository.save(gpsLog);
    }

    /**
     * Record detected violations and return their DTO representations.
     */
    private List<ViolationDetail> recordViolations(
            Bus bus, ScheduleAssignment assignment,
            ComplianceResult compliance,
            double lat, double lng, LocalDateTime detectedAt) {

        List<ViolationDetail> details = new ArrayList<>();

        // Route deviation violation.
        if (compliance.isRouteDeviation()) {
            RouteViolation violation = new RouteViolation();
            violation.setBus(bus);
            violation.setScheduleAssignment(assignment);
            violation.setViolationType(ViolationType.ROUTE_DEVIATION);
            violation.setDescription(String.format(
                    "Bus deviated %.0fm from route (threshold: %.0fm)",
                    compliance.getMinDistanceToRouteMeters(),
                    RouteComplianceUtil.DEFAULT_ROUTE_DEVIATION_METERS));
            violation.setLatitude(lat);
            violation.setLongitude(lng);
            violation.setDetectedAt(detectedAt);
            violationRepository.save(violation);
            try {
                notificationService.sendViolationNotification(bus, violation.getDescription());
            } catch (Exception e) {
                log.error("Failed to send route deviation notification", e);
            }

            details.add(ViolationDetail.builder()
                    .type(ViolationType.ROUTE_DEVIATION.name())
                    .description(violation.getDescription())
                    .latitude(lat)
                    .longitude(lng)
                    .detectedAt(detectedAt.toString())
                    .build());

            log.warn("Route deviation detected for bus {} — {}m off route",
                    bus.getId(), compliance.getMinDistanceToRouteMeters());
        }

        // Halt skipping violations.
        if (!compliance.getSkippedHaltNames().isEmpty()) {
            for (String haltName : compliance.getSkippedHaltNames()) {
                String desc = String.format(
                        "Bus skipped halt: %s", haltName);

                RouteViolation violation = new RouteViolation();
                violation.setBus(bus);
                violation.setScheduleAssignment(assignment);
                violation.setViolationType(ViolationType.HALT_SKIPPED);
                violation.setDescription(desc);
                violation.setLatitude(lat);
                violation.setLongitude(lng);
                violation.setDetectedAt(detectedAt);
                violationRepository.save(violation);
                try {
                    notificationService.sendViolationNotification(bus, violation.getDescription());
                } catch (Exception e) {
                    log.error("Failed to send halt skipped notification", e);
                }

                details.add(ViolationDetail.builder()
                        .type(ViolationType.HALT_SKIPPED.name())
                        .description(desc)
                        .latitude(lat)
                        .longitude(lng)
                        .detectedAt(detectedAt.toString())
                        .build());

                log.warn("Halt skipped by bus {}: {}",
                        bus.getId(), haltName);
            }
        }

        return details;
    }

    @Override
    @Transactional(readOnly = true)
    public ActiveBusLocationResponse getBusLocation(Integer busId) {
        GpsLog log = gpsLogRepository.findTopByBusIdOrderByRecordedAtDesc(busId)
                .orElse(null);
        if (log == null) {
            Bus bus = busRepository.findById(busId).orElse(null);
            if (bus == null) return null;
            return ActiveBusLocationResponse.builder()
                    .busId(bus.getId())
                    .registrationNumber(bus.getBusNumber())
                    .build();
        }
        
        Long routeId = null;
        String routeNumber = null;
        if (log.getScheduleAssignment() != null && log.getScheduleAssignment().getTrip() != null
            && log.getScheduleAssignment().getTrip().getTimetable() != null
            && log.getScheduleAssignment().getTrip().getTimetable().getBusRoute() != null) {
            var route = log.getScheduleAssignment().getTrip().getTimetable().getBusRoute();
            routeId = route.getId();
            routeNumber = route.getRouteNumber();
        }
        
        return ActiveBusLocationResponse.builder()
                .busId(log.getBus().getId())
                .registrationNumber(log.getBus().getBusNumber())
                .latitude(log.getLatitude())
                .longitude(log.getLongitude())
                .speed(log.getSpeed())
                .recordedAt(log.getRecordedAt())
                .routeId(routeId)
                .routeNumber(routeNumber)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<GpsLogResponse> getGpsLogs(Integer busId, LocalDate from, LocalDate to, Integer limit) {
        List<GpsLog> logs;
        if (from != null && to != null) {
            logs = gpsLogRepository.findByBusIdAndDateRange(busId, from.atStartOfDay(), to.atTime(LocalTime.MAX));
        } else if (limit != null) {
            logs = gpsLogRepository.findRecentByBusId(busId, limit);
        } else {
            logs = gpsLogRepository.findRecentByBusId(busId, 100);
        }

        return logs.stream().map(log -> GpsLogResponse.builder()
                .id(log.getId())
                .busId(log.getBus().getId())
                .registrationNumber(log.getBus().getBusNumber())
                .latitude(log.getLatitude())
                .longitude(log.getLongitude())
                .speed(log.getSpeed())
                .recordedAt(log.getRecordedAt())
                .scheduleAssignmentId(log.getScheduleAssignment() != null ? log.getScheduleAssignment().getId() : null)
                .onRoute(log.getScheduleAssignment() == null || 
                         !violationRepository.findByScheduleAssignment(log.getScheduleAssignment()).stream()
                            .anyMatch(v -> v.getViolationType() == ViolationType.ROUTE_DEVIATION 
                                           && v.getDetectedAt() != null 
                                           && log.getRecordedAt() != null 
                                           && Math.abs(java.time.Duration.between(v.getDetectedAt(), log.getRecordedAt()).toSeconds()) < 10))
                .build()
        ).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public TripHistoryResponse getActiveTrip(Integer busId) {
        Bus bus = busRepository.findById(busId)
                .orElseThrow(() -> new RuntimeException("Bus not found: " + busId));
        ScheduleAssignment activeAssignment = findActiveAssignment(bus, LocalDateTime.now());
        if (activeAssignment == null) {
            return null;
        }
        return mapToTripHistoryResponse(activeAssignment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TripHistoryResponse> getTripHistory(Integer busId, Long routeId, LocalDate from, LocalDate to) {
        LocalDate startDate = from != null ? from : LocalDate.now().minusDays(30);
        LocalDate endDate = to != null ? to : LocalDate.now();
        List<ScheduleAssignment> assignments;
        
        if (routeId != null) {
            assignments = assignmentRepository.findByRouteAndDateRange(routeId, startDate, endDate);
        } else if (busId != null) {
            assignments = assignmentRepository.findByBusAndDateRange(busId, startDate, endDate);
        } else {
            assignments = assignmentRepository.findAllByDateRangeAndStatusIn(
                    startDate, endDate, List.of(AssignmentStatus.values()));
        }
        
        return assignments.stream()
                .map(this::mapToTripHistoryResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ViolationResponse> getViolations(Integer busId, String type, LocalDate from, LocalDate to) {
        LocalDateTime start = (from != null ? from : LocalDate.now().minusDays(30)).atStartOfDay();
        LocalDateTime end = (to != null ? to : LocalDate.now()).atTime(LocalTime.MAX);
        
        List<RouteViolation> violations;
        if (busId != null) {
            violations = violationRepository.findByBusIdAndDateRange(busId, start, end);
        } else {
            violations = violationRepository.findByDateRange(start, end);
        }
        
        if (type != null && !type.isEmpty()) {
            try {
                ViolationType vType = ViolationType.valueOf(type);
                violations = violations.stream()
                        .filter(v -> v.getViolationType() == vType)
                        .collect(Collectors.toList());
            } catch (IllegalArgumentException e) {
                // Ignore invalid type
            }
        }
        
        return violations.stream()
                .map(this::mapToViolationResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ViolationResponse> getActiveViolations() {
        LocalDateTime since = LocalDateTime.now().minusHours(1);
        return violationRepository.findRecentViolations(since).stream()
                .map(this::mapToViolationResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<HaltStatusResponse> getHaltStatus(Integer busId) {
        Bus bus = busRepository.findById(busId)
                .orElseThrow(() -> new RuntimeException("Bus not found: " + busId));
        ScheduleAssignment activeAssignment = findActiveAssignment(bus, LocalDateTime.now());
        if (activeAssignment == null) {
            return new ArrayList<>();
        }
        
        var busRoute = activeAssignment.getTrip().getTimetable().getBusRoute();
        List<BusRouteHalt> routeHalts = routeHaltRepository.findByBusRouteOrderByStopOrderAsc(busRoute);
        List<GpsLog> logs = gpsLogRepository.findByScheduleAssignmentOrderByRecordedAtAsc(activeAssignment);
        
        List<HaltStatusResponse> responses = new ArrayList<>();
        
        // Add Start Terminal
        responses.add(HaltStatusResponse.builder()
                .name(busRoute.getStartLocation() + " (Start)")
                .stopOrder(0)
                .status(logs.isEmpty() ? "current" : "visited")
                .visitedAt(logs.isEmpty() ? null : logs.get(0).getRecordedAt().toString())
                .build());

        int haltCount = routeHalts.size();
        if (haltCount > 0) {
            boolean[] visited = new boolean[haltCount];
            LocalDateTime[] visitedTimes = new LocalDateTime[haltCount];
            
            for (int h = 0; h < haltCount; h++) {
                BusRouteHalt rh = routeHalts.get(h);
                double haltLat = rh.getBusHalt().getLatitude();
                double haltLng = rh.getBusHalt().getLongitude();
                
                for (GpsLog logEntry : logs) {
                    double dist = RouteComplianceUtil.haversineMeters(
                            logEntry.getLatitude(), logEntry.getLongitude(), haltLat, haltLng);
                    if (dist <= RouteComplianceUtil.DEFAULT_HALT_PROXIMITY_METERS) {
                        visited[h] = true;
                        if (visitedTimes[h] == null || logEntry.getRecordedAt().isBefore(visitedTimes[h])) {
                            visitedTimes[h] = logEntry.getRecordedAt();
                        }
                    }
                }
            }
            
            int nearestIdx = -1;
            if (!logs.isEmpty()) {
                GpsLog latestLog = logs.get(logs.size() - 1);
                double minDist = Double.MAX_VALUE;
                for (int h = 0; h < haltCount; h++) {
                    BusRouteHalt rh = routeHalts.get(h);
                    double dist = RouteComplianceUtil.haversineMeters(
                            latestLog.getLatitude(), latestLog.getLongitude(),
                            rh.getBusHalt().getLatitude(), rh.getBusHalt().getLongitude());
                    if (dist < minDist) {
                        minDist = dist;
                        nearestIdx = h;
                    }
                }
            }
            
            for (int h = 0; h < haltCount; h++) {
                BusRouteHalt rh = routeHalts.get(h);
                String status = "upcoming";
                String visitedAtStr = null;
                
                if (visited[h]) {
                    status = "visited";
                    visitedAtStr = visitedTimes[h].toString();
                } else if (nearestIdx != -1) {
                    if (h == nearestIdx) {
                        status = "current";
                    } else if (h < nearestIdx) {
                        status = "missed";
                    }
                }
                
                responses.add(HaltStatusResponse.builder()
                        .name(rh.getBusHalt().getName())
                        .stopOrder(rh.getStopOrder())
                        .status(status)
                        .visitedAt(visitedAtStr)
                        .build());
            }
        }
        
        // Add End Terminal
        responses.add(HaltStatusResponse.builder()
                .name(busRoute.getEndLocation() + " (End)")
                .stopOrder(Integer.MAX_VALUE)
                .status("upcoming")
                .visitedAt(null)
                .build());
        
        return responses;
    }

    private String formatPercentageChange(double current, double previous) {
        if (previous == 0) {
            return current > 0 ? "+100%" : "0%";
        }
        double change = ((current - previous) / previous) * 100.0;
        String sign = change > 0 ? "+" : "";
        return String.format("%s%.0f%%", sign, change);
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardSummaryResponse getDashboardSummary() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = LocalDate.now();
        LocalDateTime thirtyMinutesAgo = now.minusMinutes(30);
        List<GpsLog> latestLogs = gpsLogRepository.findLatestGpsLogs(thirtyMinutesAgo);
        
        int totalFleetCount = (int) busRepository.count();

        // Calculate count of buses with approved unavailability for today
        List<ApprovalRequest> approvedReqs = approvalRequestRepository.findByStatus(ApprovalStatus.APPROVED);
        long unavailableBusesCount = approvedReqs.stream()
                .filter(req -> req.getBus() != null)
                .filter(req -> {
                    if (req.getApprovalType() == ApprovalType.EMERGENCY) {
                        LocalDate reqDate = req.getStartDate() != null ? req.getStartDate() :
                                (req.getCreatedAt() != null ? req.getCreatedAt().toLocalDate() : LocalDate.now());
                        return reqDate.equals(today);
                    } else if (req.getApprovalType() == ApprovalType.NORMAL) {
                        if (req.getStartDate() != null && req.getEndDate() != null) {
                            return !today.isBefore(req.getStartDate()) && !today.isAfter(req.getEndDate());
                        }
                    }
                    return false;
                })
                .map(req -> req.getBus().getId())
                .distinct()
                .count();

        int activeBuses = latestLogs.size();
        // If no GPS logs are emitting yet, operational active count is totalFleetCount - unavailableBusesCount
        if (activeBuses == 0 && totalFleetCount > 0) {
            activeBuses = Math.max(0, totalFleetCount - (int) unavailableBusesCount);
        }

        LocalDateTime oneHourAgo = now.minusHours(1);
        List<RouteViolation> recentDeviations = violationRepository.findRecentViolations(oneHourAgo).stream()
                .filter(v -> v.getViolationType() == ViolationType.ROUTE_DEVIATION)
                .collect(Collectors.toList());
        
        long deviatedBusesCount = latestLogs.stream()
                .filter(log -> recentDeviations.stream()
                        .anyMatch(v -> v.getBus().getId().equals(log.getBus().getId())))
                .count();
        
        int onRouteBuses = (int) (activeBuses - deviatedBusesCount);
        if (onRouteBuses < 0) onRouteBuses = 0;
        
        int activeViolations = (int) violationRepository.countByDetectedAtAfter(oneHourAgo);
        
        List<ScheduleAssignment> todayAssignments = assignmentRepository.findByAssignmentDate(today);
        int tripsToday = todayAssignments.size();
        int completedTripsToday = (int) todayAssignments.stream()
                .filter(sa -> sa.getStatus() == AssignmentStatus.COMPLETED)
                .count();
        
        // Previous Data (24 hours ago) for percentage change calculations
        LocalDateTime oneDayAgo = now.minusDays(1);
        LocalDateTime thirtyMinsBeforeOneDayAgo = thirtyMinutesAgo.minusDays(1);
        int activeBusesYesterday = gpsLogRepository.countDistinctBusesByRecordedAtBetween(thirtyMinsBeforeOneDayAgo, oneDayAgo);

        LocalDateTime oneHourBeforeOneDayAgo = oneHourAgo.minusDays(1);
        List<RouteViolation> recentDeviationsYesterday = violationRepository.findByDateRange(oneHourBeforeOneDayAgo, oneDayAgo).stream()
                .filter(v -> v.getViolationType() == ViolationType.ROUTE_DEVIATION)
                .collect(Collectors.toList());
        long deviatedBusesYesterday = recentDeviationsYesterday.stream().map(v -> v.getBus().getId()).distinct().count();
        int onRouteBusesYesterday = (int) (activeBusesYesterday - deviatedBusesYesterday);
        if (onRouteBusesYesterday < 0) onRouteBusesYesterday = 0;

        int activeViolationsYesterday = violationRepository.findByDateRange(oneHourBeforeOneDayAgo, oneDayAgo).size();

        List<ScheduleAssignment> yesterdayAssignments = assignmentRepository.findByAssignmentDate(today.minusDays(1));
        int tripsYesterday = yesterdayAssignments.size();

        return DashboardSummaryResponse.builder()
                .activeBuses(activeBuses)
                .totalFleetCount(totalFleetCount)
                .unavailableBusesCount((int) unavailableBusesCount)
                .onRouteBuses(onRouteBuses)
                .activeViolations(activeViolations)
                .tripsToday(tripsToday)
                .completedTripsToday(completedTripsToday)
                .activeBusesChange(formatPercentageChange(activeBuses, activeBusesYesterday))
                .onRouteChange(formatPercentageChange(onRouteBuses, onRouteBusesYesterday))
                .violationChange(formatPercentageChange(activeViolations, activeViolationsYesterday))
                .tripsChange(formatPercentageChange(tripsToday, tripsYesterday))
                .build();
    }

    private TripHistoryResponse mapToTripHistoryResponse(ScheduleAssignment sa) {
        var trip = sa.getTrip();
        var timetable = trip.getTimetable();
        var route = timetable.getBusRoute();
        List<BusRouteHalt> routeHalts = routeHaltRepository.findByBusRouteOrderByStopOrderAsc(route);
        
        List<GpsLog> logs = gpsLogRepository.findByScheduleAssignmentOrderByRecordedAtAsc(sa);
        int totalHalts = routeHalts.size();
        int visitedHalts = 0;
        List<TripHistoryResponse.MissedHaltInfo> missedHalts = new ArrayList<>();
        
        boolean[] visited = new boolean[totalHalts];
        for (int h = 0; h < totalHalts; h++) {
            BusRouteHalt rh = routeHalts.get(h);
            double haltLat = rh.getBusHalt().getLatitude();
            double haltLng = rh.getBusHalt().getLongitude();
            
            for (GpsLog logEntry : logs) {
                double dist = RouteComplianceUtil.haversineMeters(
                        logEntry.getLatitude(), logEntry.getLongitude(), haltLat, haltLng);
                if (dist <= RouteComplianceUtil.DEFAULT_HALT_PROXIMITY_METERS) {
                    visited[h] = true;
                    break;
                }
            }
            if (visited[h]) {
                visitedHalts++;
            }
        }
        
        int highestVisitedIdx = -1;
        for (int h = 0; h < totalHalts; h++) {
            if (visited[h]) {
                highestVisitedIdx = h;
            }
        }
        
        if (highestVisitedIdx >= 0) {
            for (int h = 0; h <= highestVisitedIdx; h++) {
                if (!visited[h]) {
                    BusRouteHalt rh = routeHalts.get(h);
                    missedHalts.add(TripHistoryResponse.MissedHaltInfo.builder()
                            .name(rh.getBusHalt().getName())
                            .stopOrder(rh.getStopOrder())
                            .build());
                }
            }
        }
        
        long violationCount = violationRepository.countByScheduleAssignment(sa);
        
        double avgSpeed = 0.0;
        double distance = 0.0;
        if (!logs.isEmpty()) {
            avgSpeed = logs.stream().mapToDouble(GpsLog::getSpeed).average().orElse(0.0);
            
            for (int i = 0; i < logs.size() - 1; i++) {
                GpsLog log1 = logs.get(i);
                GpsLog log2 = logs.get(i + 1);
                distance += RouteComplianceUtil.haversineMeters(
                        log1.getLatitude(), log1.getLongitude(),
                        log2.getLatitude(), log2.getLongitude());
            }
            distance = distance / 1000.0;
        }
        
        LocalDateTime startDateTime = sa.getAssignmentDate().atTime(trip.getDepartureTime());
        LocalDateTime endDateTime = sa.getAssignmentDate().atTime(trip.getArrivalTime());
        
        int standardDuration = trip.getEstimatedDurationMinutes() != null && trip.getEstimatedDurationMinutes() > 0
                ? trip.getEstimatedDurationMinutes()
                : (int) java.time.Duration.between(trip.getDepartureTime(), trip.getArrivalTime()).toMinutes();
        if (standardDuration <= 0) standardDuration = 60;

        int actualDuration = standardDuration;
        if (logs.size() > 1) {
            LocalDateTime actualStart = logs.get(0).getRecordedAt();
            LocalDateTime actualEnd = logs.get(logs.size() - 1).getRecordedAt();
            actualDuration = (int) java.time.Duration.between(actualStart, actualEnd).toMinutes();
        }

        int delayMins = sa.getDelayMinutes() != null && sa.getDelayMinutes() > 0 
                ? sa.getDelayMinutes() 
                : Math.max(0, actualDuration - standardDuration);
        
        boolean isDelayed = delayMins > 0;
        if (isDelayed) {
            violationCount += 1;
        }

        String delayStatusStr = isDelayed ? ("+" + delayMins + "m Late") : "On-Time";

        return TripHistoryResponse.builder()
                .id(sa.getId())
                .busId(sa.getBus() != null ? sa.getBus().getId() : null)
                .routeId(route != null ? route.getId() : null)
                .registrationNumber(sa.getBus() != null ? sa.getBus().getBusNumber() : "UNKNOWN")
                .routeNumber(route != null ? route.getRouteNumber() : "N/A")
                .tripCode(trip.getTripCode())
                .tripLabel(route != null ? (route.getStartLocation() + " to " + route.getEndLocation()) : trip.getTripLabel())
                .startLocation(route != null ? route.getStartLocation() : "Start")
                .endLocation(route != null ? route.getEndLocation() : "End")
                .startTime(startDateTime)
                .endTime(endDateTime)
                .status(sa.getStatus().name())
                .haltsVisited(visitedHalts)
                .totalHalts(totalHalts)
                .violationCount((int) violationCount)
                .distanceCovered(Math.round(distance * 10.0) / 10.0)
                .avgSpeed(Math.round(avgSpeed * 10.0) / 10.0)
                .gpsLogCount(logs.size())
                .delayMinutes(delayMins)
                .standardDurationMinutes(standardDuration)
                .actualDurationMinutes(actualDuration)
                .isDelayed(isDelayed)
                .delayStatus(delayStatusStr)
                .missedHalts(missedHalts)
                .build();
    }

    private ViolationResponse mapToViolationResponse(RouteViolation v) {
        String routeNumber = "Unknown";
        if (v.getScheduleAssignment() != null && v.getScheduleAssignment().getTrip() != null
            && v.getScheduleAssignment().getTrip().getTimetable() != null
            && v.getScheduleAssignment().getTrip().getTimetable().getBusRoute() != null) {
            routeNumber = v.getScheduleAssignment().getTrip().getTimetable().getBusRoute().getRouteNumber();
        }
        
        String mappedSeverity = "WARNING";
        if (v.getViolationType() == ViolationType.ROUTE_DEVIATION || v.getViolationType() == ViolationType.SUSPICIOUS_MOVEMENT) {
            mappedSeverity = "CRITICAL";
        }
        
        return ViolationResponse.builder()
                .id(v.getId())
                .busId(v.getBus().getId())
                .registrationNumber(v.getBus().getBusNumber())
                .routeNumber(routeNumber)
                .type(v.getViolationType().name())
                .severity(mappedSeverity)
                .message(v.getDescription())
                .latitude(v.getLatitude())
                .longitude(v.getLongitude())
                .detectedAt(v.getDetectedAt())
                .timestamp(v.getDetectedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<GpsLogResponse> getGpsLogsByScheduleAssignment(Integer scheduleAssignmentId) {
        ScheduleAssignment assignment = assignmentRepository.findById(scheduleAssignmentId)
                .orElseThrow(() -> new RuntimeException("Schedule assignment not found: " + scheduleAssignmentId));

        List<GpsLog> logs = gpsLogRepository.findByScheduleAssignmentOrderByRecordedAtAsc(assignment);

        return logs.stream().map(log -> GpsLogResponse.builder()
                .id(log.getId())
                .busId(log.getBus().getId())
                .registrationNumber(log.getBus().getBusNumber())
                .latitude(log.getLatitude())
                .longitude(log.getLongitude())
                .speed(log.getSpeed())
                .recordedAt(log.getRecordedAt())
                .scheduleAssignmentId(scheduleAssignmentId)
                .onRoute(log.getScheduleAssignment() == null || 
                         !violationRepository.findByScheduleAssignment(log.getScheduleAssignment()).stream()
                            .anyMatch(v -> v.getViolationType() == ViolationType.ROUTE_DEVIATION 
                                           && v.getDetectedAt() != null 
                                           && log.getRecordedAt() != null 
                                           && Math.abs(java.time.Duration.between(v.getDetectedAt(), log.getRecordedAt()).toSeconds()) < 10))
                .build()
        ).collect(Collectors.toList());
    }
}
