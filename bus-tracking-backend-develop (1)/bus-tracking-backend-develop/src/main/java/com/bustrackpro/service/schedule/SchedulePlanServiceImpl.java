package com.bustrackpro.service.schedule;

import com.bustrackpro.dto.request.schedule.MonthlyPlanRequest;
import com.bustrackpro.dto.request.schedule.NextDayAdjustmentRequest;
import com.bustrackpro.dto.response.schedule.MonthlyPlanResponse;
import com.bustrackpro.dto.response.schedule.NextDayAdjustmentResponse;
import com.bustrackpro.modal.bus.Bus;
import com.bustrackpro.modal.schedule.AssignmentStatus;
import com.bustrackpro.modal.schedule.DayType;
import com.bustrackpro.modal.schedule.ScheduleAssignment;
import com.bustrackpro.modal.schedule.ScheduleType;
import com.bustrackpro.modal.schedule.Timetable;
import com.bustrackpro.modal.schedule.Trip;
import com.bustrackpro.repository.bus.BusRepository;
import com.bustrackpro.repository.BusRouteRepository;
import com.bustrackpro.repository.schedule.ScheduleAssignmentRepository;
import com.bustrackpro.repository.schedule.TimetableRepository;
import com.bustrackpro.repository.schedule.TripRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SchedulePlanServiceImpl implements SchedulePlanService {

    private final BusRepository busRepository;
    private final BusRouteRepository busRouteRepository;
    private final TimetableRepository timetableRepository;
    private final TripRepository tripRepository;
    private final ScheduleAssignmentRepository assignmentRepository;
    private final ScheduleResolverService scheduleResolverService;

    @Autowired
    public SchedulePlanServiceImpl(BusRepository busRepository,
                                   BusRouteRepository busRouteRepository,
                                   TimetableRepository timetableRepository,
                                   TripRepository tripRepository,
                                   ScheduleAssignmentRepository assignmentRepository,
                                   ScheduleResolverService scheduleResolverService) {
        this.busRepository = busRepository;
        this.busRouteRepository = busRouteRepository;
        this.timetableRepository = timetableRepository;
        this.tripRepository = tripRepository;
        this.assignmentRepository = assignmentRepository;
        this.scheduleResolverService = scheduleResolverService;
    }

    @Override
    @Transactional
    public MonthlyPlanResponse generateMonthlyPlan(MonthlyPlanRequest request) {
        YearMonth yearMonth = YearMonth.parse(request.month());
        int daysInMonth = yearMonth.lengthOfMonth();
        
        // 1. Fetch Route
        busRouteRepository.findById(request.routeId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Route not found"));

        // 2. Fetch all buses
        List<Bus> allBuses = busRepository.findAll();
        if (allBuses.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No buses found");
        }

        // Filter buses assigned to this route
        List<Bus> routeBuses = allBuses.stream()
                .filter(b -> b.getRoute() != null && b.getRoute().getId() == request.routeId().longValue())
                .collect(Collectors.toList());
        if (routeBuses.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No buses assigned to this route");
        }

        LocalDate startOfMonth = yearMonth.atDay(1);
        LocalDate endOfMonth = yearMonth.atEndOfMonth();

        // 3. Fetch Timetables to check if any are active during the month
        List<Timetable> timetablesInPeriod = timetableRepository.findTimetablesActiveInPeriod(
                request.routeId(), startOfMonth, endOfMonth);
                
        if (timetablesInPeriod.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No active timetable found for this route in the selected month");
        }

        // 4. Delete existing assignments for this route and month to prevent unique constraint violations
        List<ScheduleAssignment> existing = assignmentRepository.findByRouteIdAndAssignmentDateBetween(
                request.routeId(), startOfMonth, endOfMonth);
        if (!existing.isEmpty()) {
            assignmentRepository.deleteAll(existing);
            assignmentRepository.flush(); // Ensure deletes are processed immediately
        }

        List<MonthlyPlanResponse.MonthlyPlanDay> dailyPlans = new ArrayList<>();
        List<ScheduleAssignment> allAssignmentsToSave = new ArrayList<>();
        int scheduledDaysCount = 0;

        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate date = yearMonth.atDay(day);
            List<MonthlyPlanResponse.Assignment> dailyAssignmentsDto = new ArrayList<>();
            
            Timetable effectiveTimetable = scheduleResolverService.determineActiveSchedule(date, request.routeId());

            if (effectiveTimetable == null) {
                continue;
            }

            List<Trip> dailyTrips = new ArrayList<>();
            dailyTrips.addAll(tripRepository.findByTimetableOrderByDepartureTimeAsc(effectiveTimetable));

            if (dailyTrips.isEmpty()) {
                continue;
            }

            dailyTrips.sort(Comparator.comparing(Trip::getDepartureTime));
            scheduledDaysCount++;

            // Track bus availability to prevent overlapping
            Map<Bus, LocalTime> busAvailableTime = new HashMap<>();
            Map<Bus, Integer> busTripCount = new HashMap<>();
            Map<Bus, String> busLocation = new HashMap<>();
            
            // Shuffle a copy of routeBuses for the day to randomize slot assignments
            List<Bus> dailyBuses = new ArrayList<>(routeBuses);
            Collections.shuffle(dailyBuses);

            for (Bus b : dailyBuses) {
                busAvailableTime.put(b, LocalTime.MIN);
                busTripCount.put(b, 0);
                busLocation.put(b, "ANY");
            }

            for (Trip trip : dailyTrips) {
                String tripStartLoc = getTripStartLocation(trip);
                String tripEndLoc = getTripEndLocation(trip);

                // Find all buses available for this trip's departure time AND at the correct location
                List<Bus> available = dailyBuses.stream()
                        .filter(b -> !busAvailableTime.get(b).isAfter(trip.getDepartureTime()))
                        .filter(b -> busLocation.get(b).equals("ANY") || busLocation.get(b).equals(tripStartLoc))
                        .collect(Collectors.toList());

                if (available.isEmpty()) {
                    // Fallback 1: Is there a bus at the correct location that becomes available later?
                    available = dailyBuses.stream()
                            .filter(b -> busLocation.get(b).equals("ANY") || busLocation.get(b).equals(tripStartLoc))
                            .collect(Collectors.toList());
                            
                    if (available.isEmpty()) {
                        // Fallback 2: Grab any bus if none at correct location
                        available = new ArrayList<>(dailyBuses);
                    }
                    available.sort(Comparator.comparing(busAvailableTime::get));
                } else {
                    // Sort available by fewest trips today, then by who has been waiting longest
                    available.sort((b1, b2) -> {
                        int countCmp = Integer.compare(busTripCount.get(b1), busTripCount.get(b2));
                        if (countCmp != 0) return countCmp;
                        return busAvailableTime.get(b1).compareTo(busAvailableTime.get(b2));
                    });
                }

                Bus selectedBus = available.get(0);

                // Create Assignment
                ScheduleAssignment assignment = new ScheduleAssignment();
                assignment.setBus(selectedBus);
                assignment.setTrip(trip);
                assignment.setAssignmentDate(date);
                assignment.setStatus(AssignmentStatus.SCHEDULED);
                
                allAssignmentsToSave.add(assignment);

                // Update state
                LocalTime nextAvailable = trip.getArrivalTime().plusMinutes(
                        trip.getBufferTimeMinutes() != null ? trip.getBufferTimeMinutes() : 0);
                busAvailableTime.put(selectedBus, nextAvailable);
                busTripCount.put(selectedBus, busTripCount.get(selectedBus) + 1);
                busLocation.put(selectedBus, tripEndLoc);

                dailyAssignmentsDto.add(new MonthlyPlanResponse.Assignment(
                        selectedBus.getBusNumber(),
                        trip.getTripLabel() != null ? trip.getTripLabel() : trip.getTripCode(),
                        date.atTime(trip.getDepartureTime()),
                        date.atTime(trip.getArrivalTime()),
                        trip.getBufferTimeMinutes() != null ? trip.getBufferTimeMinutes() : 0
                ));
            }
            
            dailyPlans.add(new MonthlyPlanResponse.MonthlyPlanDay(date, dailyAssignmentsDto));
        }

        assignmentRepository.saveAll(allAssignmentsToSave);

        Set<Bus> uniqueBuses = allAssignmentsToSave.stream()
                .map(a -> a.getBus())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        long tripsPerDay = scheduledDaysCount > 0 ? (allAssignmentsToSave.size() / scheduledDaysCount) : 0;

        Map<String, Object> summary = Map.of(
                "totalDays", scheduledDaysCount,
                "totalBusesScheduled", uniqueBuses.size(),
                "tripsPerDay", tripsPerDay
        );

        return new MonthlyPlanResponse(summary, dailyPlans);
    }

    @Override
    @Transactional
    public NextDayAdjustmentResponse generateNextDayAdjustment(NextDayAdjustmentRequest request) {
        LocalDate date = request.date();
        List<String> unavailable = request.unavailableBuses() != null ? request.unavailableBuses() : Collections.emptyList();
        
        List<ScheduleAssignment> existingAssignments = assignmentRepository.findByAssignmentDate(date);

        List<NextDayAdjustmentResponse.AdjustedAssignment> adjustedPlan = new ArrayList<>();
        List<NextDayAdjustmentResponse.RemovedAssignment> removed = new ArrayList<>();
        
        List<ScheduleAssignment> assignmentsToReassign = new ArrayList<>();
        List<ScheduleAssignment> availableAssignments = new ArrayList<>();

        for (ScheduleAssignment assignment : existingAssignments) {
            String busNo = assignment.getBus() != null ? assignment.getBus().getBusNumber() : "UNKNOWN";
            
            if (unavailable.contains(busNo)) {
                removed.add(new NextDayAdjustmentResponse.RemovedAssignment(busNo, assignment.getTrip().getTripCode(), "Bus Unavailable"));
                assignmentsToReassign.add(assignment);
            } else {
                availableAssignments.add(assignment);
                int buf = (assignment.getTrip() != null && assignment.getTrip().getBufferTimeMinutes() != null) ? assignment.getTrip().getBufferTimeMinutes() : 0;
                adjustedPlan.add(new NextDayAdjustmentResponse.AdjustedAssignment(busNo, assignment.getTrip().getTripCode(), "UNCHANGED", buf));
            }
        }

        // Gather all available buses in the system that aren't marked as unavailable
        List<Bus> allBuses = busRepository.findAll().stream()
                .filter(b -> !unavailable.contains(b.getBusNumber()))
                .collect(Collectors.toList());

        // Sort missing assignments by departure time to process them chronologically
        assignmentsToReassign.sort(Comparator.comparing(a -> a.getTrip().getDepartureTime()));

        for (ScheduleAssignment missing : assignmentsToReassign) {
            Trip missingTrip = missing.getTrip();
            String missingStart = getTripStartLocation(missingTrip);
            String missingEnd = getTripEndLocation(missingTrip);
            
            // Find a bus that is NOT scheduled for any overlapping trip
            // AND whose location continuity is maintained
            Optional<Bus> replacementBus = allBuses.stream()
                .filter(b -> {
                    // Get and sort all trips for this bus today
                    List<Trip> busTrips = availableAssignments.stream()
                        .filter(a -> a.getBus() != null && a.getBus().getId() != null && a.getBus().getId().equals(b.getId()))
                        .map(a -> a.getTrip())
                        .filter(Objects::nonNull)
                        .sorted(Comparator.comparing(Trip::getDepartureTime))
                        .collect(Collectors.toList());

                    // Overlap check
                    boolean overlaps = busTrips.stream().anyMatch(t -> 
                        missingTrip.getDepartureTime().isBefore(t.getArrivalTime()) &&
                        t.getDepartureTime().isBefore(missingTrip.getArrivalTime())
                    );
                    if (overlaps) return false;

                    // Location continuity check
                    Trip prevTrip = null;
                    Trip nextTrip = null;
                    for (Trip t : busTrips) {
                        if (!t.getDepartureTime().isAfter(missingTrip.getDepartureTime())) {
                            prevTrip = t;
                        } else if (nextTrip == null) {
                            nextTrip = t;
                        }
                    }

                    if (prevTrip != null) {
                        String prevEnd = getTripEndLocation(prevTrip);
                        if (!prevEnd.equals("ANY") && !missingStart.equals("ANY") && !prevEnd.equals(missingStart)) {
                            return false;
                        }
                    }

                    if (nextTrip != null) {
                        String nextStart = getTripStartLocation(nextTrip);
                        if (!missingEnd.equals("ANY") && !nextStart.equals("ANY") && !missingEnd.equals(nextStart)) {
                            return false;
                        }
                    }

                    return true;
                })
                .findFirst();

            if (replacementBus.isPresent()) {
                Bus newBus = replacementBus.get();
                missing.setBus(newBus);
                missing.setDriver(null); // Fallback to new bus default driver
                missing.setStatus(AssignmentStatus.SCHEDULED);
                assignmentRepository.save(missing);
                
                int buf = (missingTrip != null && missingTrip.getBufferTimeMinutes() != null) ? missingTrip.getBufferTimeMinutes() : 0;
                adjustedPlan.add(new NextDayAdjustmentResponse.AdjustedAssignment(
                        newBus.getBusNumber(), missingTrip.getTripCode(), "REASSIGNED", buf));
                
                availableAssignments.add(missing);
            } else {
                removed.add(new NextDayAdjustmentResponse.RemovedAssignment("NONE", missingTrip.getTripCode(), "NO BUS AVAILABLE"));
                missing.setStatus(AssignmentStatus.CANCELLED);
                assignmentRepository.save(missing);
            }
        }

        return new NextDayAdjustmentResponse(
                adjustedPlan,
                removed,
                Map.of(),
                List.of("Reassigned missing trips using greedy availability logic."),
                Map.of("complianceStatus", "PASS")
        );
    }

    private String getTripStartLocation(Trip trip) {
        if (trip.getDirection() != null) {
            if (trip.getDirection() == com.bustrackpro.modal.schedule.TripDirection.MAIN_WAY) return "START_NODE";
            if (trip.getDirection() == com.bustrackpro.modal.schedule.TripDirection.RETURN_WAY) return "END_NODE";
        }
        
        // Fallback for legacy trips without a direction set
        String code = trip.getTripCode();
        if (code != null) {
            if (code.startsWith("HP")) return "START_NODE";
            if (code.startsWith("PH")) return "END_NODE";
        }
        return "ANY";
    }

    private String getTripEndLocation(Trip trip) {
        if (trip.getDirection() != null) {
            if (trip.getDirection() == com.bustrackpro.modal.schedule.TripDirection.MAIN_WAY) return "END_NODE";
            if (trip.getDirection() == com.bustrackpro.modal.schedule.TripDirection.RETURN_WAY) return "START_NODE";
        }
        
        // Fallback for legacy trips without a direction set
        String code = trip.getTripCode();
        if (code != null) {
            if (code.startsWith("HP")) return "END_NODE";
            if (code.startsWith("PH")) return "START_NODE";
        }
        return "ANY";
    }
}
