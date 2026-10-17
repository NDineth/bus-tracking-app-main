package com.bustrackpro.service.bus;

import com.bustrackpro.dto.request.bus.BusRequest;
import com.bustrackpro.dto.response.bus.BusResponse;
import com.bustrackpro.dto.response.bus.FleetSummaryResponse;
import com.bustrackpro.dto.response.user.UserResponse;
import com.bustrackpro.modal.approval.ApprovalRequest;
import com.bustrackpro.modal.approval.ApprovalStatus;
import com.bustrackpro.modal.approval.ApprovalType;
import com.bustrackpro.modal.bus.Bus;
import com.bustrackpro.modal.user.UserRole;
import com.bustrackpro.modal.user.Users;
import com.bustrackpro.repository.BusRouteRepository;
import com.bustrackpro.repository.approval.ApprovalRequestRepository;
import com.bustrackpro.repository.bus.BusRepository;
import com.bustrackpro.repository.schedule.ScheduleAssignmentRepository;
import com.bustrackpro.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementation of Bus service.
 */
@Service
@RequiredArgsConstructor
public class BusServiceImpl implements BusService {

    private final BusRepository busRepository;
    private final UserRepository userRepository;
    private final ScheduleAssignmentRepository assignmentRepository;
    private final BusRouteRepository routeRepository;
    private final ApprovalRequestRepository approvalRequestRepository;

    @Override
    public BusResponse addBus(BusRequest busRequest) {
        Bus bus = new Bus();
        mapRequestToEntity(busRequest, bus);
        Bus savedBus = busRepository.save(bus);
        return mapToBusResponse(savedBus);
    }

    @Override
    public List<BusResponse> getAllBuses() {
        org.springframework.security.core.Authentication authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof com.bustrackpro.config.jwt.UserDetailsImpl) {
            com.bustrackpro.config.jwt.UserDetailsImpl userDetails = (com.bustrackpro.config.jwt.UserDetailsImpl) authentication.getPrincipal();
            boolean isDriver = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("BUS_DRIVER"));
            if (isDriver) {
                int driverId = userDetails.getId();
                return busRepository.findAll().stream()
                        .filter(bus -> bus.getDriver() != null && bus.getDriver().getId() == driverId)
                        .map(this::mapToBusResponse)
                        .collect(Collectors.toList());
            }
        }
        return busRepository.findAll().stream()
                .map(this::mapToBusResponse)
                .collect(Collectors.toList());
    }

    @Override
    public BusResponse getBusById(Integer id) {
        Bus bus = busRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bus not found"));
        return mapToBusResponse(bus);
    }

    @Override
    public BusResponse updateBus(Integer id, BusRequest busRequest) {
        Bus bus = busRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bus not found"));

        mapRequestToEntity(busRequest, bus);
        Bus updatedBus = busRepository.save(bus);
        return mapToBusResponse(updatedBus);
    }

    @Override
    public void deleteBus(Integer id) {
        if (!busRepository.existsById(id)) {
            throw new RuntimeException("Bus not found");
        }
        busRepository.deleteById(id);
    }

    @Override
    public FleetSummaryResponse getFleetSummary(Integer ownerId, String dateStr) {
        LocalDate date = (dateStr != null && !dateStr.trim().isEmpty()) ? LocalDate.parse(dateStr) : LocalDate.now();

        List<Bus> allBuses = busRepository.findAll();
        if (ownerId != null) {
            allBuses = allBuses.stream()
                    .filter(b -> b.getOwner() != null && ownerId.equals(b.getOwner().getId()))
                    .collect(Collectors.toList());
        }

        int totalFleetCount = allBuses.size();
        Set<Integer> filteredBusIds = allBuses.stream().map(Bus::getId).collect(Collectors.toSet());

        // Get all approved requests
        List<ApprovalRequest> approvedRequests = approvalRequestRepository.findByStatus(ApprovalStatus.APPROVED);
        List<FleetSummaryResponse.UnavailableBusDetail> unavailableDetails = new ArrayList<>();
        Set<Integer> unavailableBusIds = new HashSet<>();

        for (ApprovalRequest req : approvedRequests) {
            if (req.getBus() == null || !filteredBusIds.contains(req.getBus().getId())) {
                continue;
            }
            if (isRequestApplicableOnDate(req, date)) {
                unavailableBusIds.add(req.getBus().getId());
                unavailableDetails.add(FleetSummaryResponse.UnavailableBusDetail.builder()
                        .busId(req.getBus().getId())
                        .busNumber(req.getBus().getBusNumber())
                        .reason(req.getReason())
                        .approvalType(req.getApprovalType() != null ? req.getApprovalType().name() : null)
                        .emergencyType(req.getEmergencyType() != null ? req.getEmergencyType().name() : null)
                        .build());
            }
        }

        int unavailableBusCount = unavailableBusIds.size();
        int activeFleetCount = Math.max(0, totalFleetCount - unavailableBusCount);

        // Count pending requests
        List<ApprovalRequest> pendingRequests = approvalRequestRepository.findByStatus(ApprovalStatus.PENDING);
        long pendingCount = pendingRequests.stream()
                .filter(req -> req.getBus() != null && filteredBusIds.contains(req.getBus().getId()))
                .filter(req -> isRequestApplicableOnDate(req, date))
                .count();

        return FleetSummaryResponse.builder()
                .totalFleetCount(totalFleetCount)
                .activeFleetCount(activeFleetCount)
                .unavailableBusCount(unavailableBusCount)
                .pendingRequestCount((int) pendingCount)
                .date(date.toString())
                .unavailableBuses(unavailableDetails)
                .build();
    }

    private boolean isRequestApplicableOnDate(ApprovalRequest req, LocalDate targetDate) {
        if (req.getApprovalType() == ApprovalType.EMERGENCY) {
            LocalDate reqDate = req.getStartDate() != null ? req.getStartDate() :
                    (req.getCreatedAt() != null ? req.getCreatedAt().toLocalDate() : LocalDate.now());
            return reqDate.equals(targetDate);
        } else if (req.getApprovalType() == ApprovalType.NORMAL) {
            if (req.getStartDate() != null && req.getEndDate() != null) {
                return !targetDate.isBefore(req.getStartDate()) && !targetDate.isAfter(req.getEndDate());
            }
        }
        return false;
    }

    private void mapRequestToEntity(BusRequest request, Bus bus) {
        bus.setBusNumber(request.getBusNumber());
        bus.setCapacity(request.getCapacity());
        bus.setModel(request.getModel());

        if (request.getDriverId() != null) {
            Users driver = userRepository.findById(request.getDriverId())
                    .orElseThrow(() -> new RuntimeException("Driver not found with id: " + request.getDriverId()));
            
            if (driver.getRole() != UserRole.BUS_DRIVER) {
                throw new RuntimeException("Assigned user must be a BUS_DRIVER");
            }

            // Check if this driver is already assigned to another bus
            busRepository.findAll().stream()
                .filter(b -> b.getDriver() != null && b.getDriver().getId() == request.getDriverId() && !b.getId().equals(bus.getId()))
                .findAny()
                .ifPresent(b -> {
                    throw new RuntimeException("Driver " + driver.getFirstName() + " " + driver.getLastName() + " is already assigned to bus: " + b.getBusNumber());
                });

            bus.setDriver(driver);
        } else {
            bus.setDriver(null);
        }

        if (request.getOwnerId() != null) {
            Users owner = userRepository.findById(request.getOwnerId())
                    .orElseThrow(() -> new RuntimeException("Owner not found with id: " + request.getOwnerId()));
            bus.setOwner(owner);
        } else {
            bus.setOwner(null);
        }

        if (request.getRouteId() != null) {
            com.bustrackpro.modal.route.BusRoute route = routeRepository.findById(request.getRouteId())
                    .orElseThrow(() -> new RuntimeException("Route not found with id: " + request.getRouteId()));
            bus.setRoute(route);
        } else {
            bus.setRoute(null);
        }
    }

    private BusResponse mapToBusResponse(Bus bus) {
        UserResponse driver = null;
        if (bus.getDriver() != null) {
            driver = mapToUserResponse(bus.getDriver());
        }

        UserResponse owner = null;
        if (bus.getOwner() != null) {
            owner = mapToUserResponse(bus.getOwner());
        }

        // Fetch today's assignments for this bus to populate route, nextTurn, and turns
        LocalDate today = LocalDate.now();
        List<com.bustrackpro.modal.schedule.ScheduleAssignment> assignments = assignmentRepository.findByBusIdAndDateAndStatusIn(
            bus.getId(), today, List.of(
                com.bustrackpro.modal.schedule.AssignmentStatus.SCHEDULED,
                com.bustrackpro.modal.schedule.AssignmentStatus.IN_PROGRESS,
                com.bustrackpro.modal.schedule.AssignmentStatus.COMPLETED
            )
        );

        String route = "No Active Route";
        String nextTurn = "N/A";
        List<String> turns = new java.util.ArrayList<>();

        if (assignments != null && !assignments.isEmpty()) {
            // Sort by departure time
            assignments.sort((a, b) -> a.getTrip().getDepartureTime().compareTo(b.getTrip().getDepartureTime()));
            
            // Map turns
            java.time.format.DateTimeFormatter timeFormatter = java.time.format.DateTimeFormatter.ofPattern("hh:mm a");
            for (com.bustrackpro.modal.schedule.ScheduleAssignment sa : assignments) {
                if (sa.getTrip() != null && sa.getTrip().getDepartureTime() != null) {
                    turns.add(sa.getTrip().getDepartureTime().format(timeFormatter));
                }
            }

            // Find next turn
            LocalTime now = LocalTime.now();
            com.bustrackpro.modal.schedule.ScheduleAssignment nextSa = assignments.stream()
                .filter(sa -> sa.getTrip() != null && sa.getTrip().getDepartureTime() != null && sa.getTrip().getDepartureTime().isAfter(now))
                .findFirst()
                .orElse(assignments.get(0)); // fallback to first turn if all are past
            
            if (nextSa.getTrip() != null && nextSa.getTrip().getDepartureTime() != null) {
                nextTurn = nextSa.getTrip().getDepartureTime().format(timeFormatter);
            }
            
            if (nextSa.getTrip() != null) {
                if (nextSa.getTrip().getTimetable() != null && nextSa.getTrip().getTimetable().getBusRoute() != null) {
                    com.bustrackpro.modal.route.BusRoute br = nextSa.getTrip().getTimetable().getBusRoute();
                    route = br.getRouteNumber() + " (" + br.getStartLocation() + " - " + br.getEndLocation() + ")";
                } else if (nextSa.getTrip().getTripLabel() != null) {
                    route = nextSa.getTrip().getTripLabel();
                }
            }
        }

        Integer routeId = null;
        if (bus.getRoute() != null) {
            routeId = (int) bus.getRoute().getId();
            route = bus.getRoute().getRouteNumber() + " (" + bus.getRoute().getStartLocation() + " - " + bus.getRoute().getEndLocation() + ")";
        }

        // Determine bus availability and status from approval requests
        String status = "IDLE";
        String availabilityStatus = "AVAILABLE";
        String unavailabilityReason = null;

        List<ApprovalRequest> approvedReqs = approvalRequestRepository.findByBusIdAndStatus(bus.getId(), ApprovalStatus.APPROVED);
        ApprovalRequest activeApprovedReq = approvedReqs.stream()
                .filter(req -> isRequestApplicableOnDate(req, today))
                .findFirst()
                .orElse(null);

        if (activeApprovedReq != null) {
            status = "UNAVAILABLE";
            availabilityStatus = "UNAVAILABLE";
            unavailabilityReason = activeApprovedReq.getReason() != null ? activeApprovedReq.getReason() :
                    ("Approved " + activeApprovedReq.getApprovalType() + " Unavailability");
        } else {
            List<ApprovalRequest> pendingReqs = approvalRequestRepository.findByBusIdAndStatus(bus.getId(), ApprovalStatus.PENDING);
            boolean hasPendingToday = pendingReqs.stream().anyMatch(req -> isRequestApplicableOnDate(req, today));

            if (hasPendingToday) {
                status = "PENDING_APPROVAL";
            } else if (assignments != null && !assignments.isEmpty()) {
                status = "ACTIVE";
            }
        }

        return BusResponse.builder()
                .id(bus.getId())
                .busNumber(bus.getBusNumber())
                .capacity(bus.getCapacity())
                .model(bus.getModel())
                .driver(driver)
                .owner(owner)
                .routeId(routeId)
                .route(route)
                .nextTurn(nextTurn)
                .turns(turns)
                .status(status)
                .availabilityStatus(availabilityStatus)
                .unavailabilityReason(unavailabilityReason)
                .build();
    }

    private UserResponse mapToUserResponse(Users user) {
        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getContactNumber(),
                user.getRole(),
                user.getStatus()
        );
    }
}

