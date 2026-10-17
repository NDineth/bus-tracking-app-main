package com.bustrackpro.service.schedule;

import com.bustrackpro.modal.bus.Bus;
import com.bustrackpro.modal.bus.BusDriver;
import com.bustrackpro.modal.route.BusRoute;
import com.bustrackpro.modal.schedule.AssignmentStatus;
import com.bustrackpro.modal.schedule.DayType;
import com.bustrackpro.modal.schedule.ScheduleAssignment;
import com.bustrackpro.modal.schedule.ScheduleType;
import com.bustrackpro.modal.schedule.Timetable;
import com.bustrackpro.modal.schedule.Trip;
import com.bustrackpro.repository.BusRouteRepository;
import com.bustrackpro.repository.bus.BusRepository;
import com.bustrackpro.repository.schedule.BusDriverRepository;
import com.bustrackpro.repository.schedule.ScheduleAssignmentRepository;
import com.bustrackpro.repository.schedule.TimetableRepository;
import com.bustrackpro.repository.schedule.TripRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for automatically generating bus schedules.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AutoScheduleService {

    private static final int DRIVER_REST_TIME_MINUTES = 30;
    private static final int MAX_DAILY_DRIVING_MINUTES = 480; // 8 hours

    private final BusRouteRepository busRouteRepository;
    private final TimetableRepository timetableRepository;
    private final TripRepository tripRepository;
    private final BusRepository busRepository;
    private final BusDriverRepository busDriverRepository;
    private final ScheduleAssignmentRepository assignmentRepository;
    private final ScheduleResolverService scheduleResolverService;
    private final com.bustrackpro.service.notification.NotificationService notificationService;

    private final Random random = new Random();

    /**
     * Generates the daily schedule for all routes for a given date.
     *
     * @param targetDate the date to generate the schedule for
     */
    @Transactional
    public void generateDailySchedule(LocalDate targetDate) {
        log.info("Starting auto-schedule generation for date: {}", targetDate);

        // 1. Fetch all active routes
        List<BusRoute> routes = busRouteRepository.findAll();

        // 2. Fetch all available resources
        List<Bus> allBuses = busRepository.findAll();
        List<BusDriver> allDrivers = busDriverRepository.findAll();

        if (allBuses.isEmpty() || allDrivers.isEmpty()) {
            log.warn("Cannot generate schedule: No buses or drivers available in the system.");
            return;
        }

        // Initialize state trackers
        Map<Integer, LocalTime> busAvailability = new HashMap<>();
        Map<Integer, LocalTime> driverAvailability = new HashMap<>();
        Map<Integer, Integer> driverDailyMinutes = new HashMap<>();

        for (Bus bus : allBuses) {
            busAvailability.put(bus.getId(), LocalTime.MIDNIGHT);
        }
        for (BusDriver driver : allDrivers) {
            driverAvailability.put(driver.getId(), LocalTime.MIDNIGHT);
            driverDailyMinutes.put(driver.getId(), 0);
        }

        // 3. Process each route
        for (BusRoute route : routes) {
            // Find effective active timetable for this route and date based on priority
            Timetable effectiveTimetable = scheduleResolverService.determineActiveSchedule(targetDate, route.getId());

            if (effectiveTimetable == null) {
                log.info("No active timetable found for route {} on {}", route.getId(), targetDate);
                continue;
            }

            // Filter buses assigned to this route
            List<Bus> routeBuses = allBuses.stream()
                    .filter(b -> b.getRoute() != null && b.getRoute().getId() == route.getId())
                    .collect(Collectors.toList());

            if (routeBuses.isEmpty()) {
                log.warn("No buses assigned to route {}. Skipping auto-scheduling for this route.", route.getRouteNumber());
                continue;
            }

            // Fetch trips sorted by departure time
            List<Trip> trips = tripRepository.findByTimetableOrderByDepartureTimeAsc(effectiveTimetable);

            for (Trip trip : trips) {
                // Check if assignment already exists to avoid duplicates
                Optional<ScheduleAssignment> existing = assignmentRepository
                        .findByTripAndAssignmentDate(trip, targetDate);
                if (existing.isPresent()) {
                    log.info("Assignment already exists for trip {} on {}", trip.getId(), targetDate);
                    continue;
                }

                assignTrip(trip, targetDate, routeBuses, allDrivers, busAvailability, driverAvailability, driverDailyMinutes);
            }
        }
        
        // 4. Dispatch daily timetable notifications directly to bus owners
        dispatchDailyTimetablesToOwners(targetDate);
        log.info("Auto-schedule generation completed for date: {}", targetDate);
    }

    private void dispatchDailyTimetablesToOwners(LocalDate targetDate) {
        List<ScheduleAssignment> assignments = assignmentRepository.findByAssignmentDate(targetDate);
        if (assignments.isEmpty()) return;

        Map<com.bustrackpro.modal.user.Users, List<ScheduleAssignment>> ownerAssignmentsMap = assignments.stream()
                .filter(sa -> sa.getBus() != null && sa.getBus().getOwner() != null)
                .collect(Collectors.groupingBy(sa -> sa.getBus().getOwner()));

        for (Map.Entry<com.bustrackpro.modal.user.Users, List<ScheduleAssignment>> entry : ownerAssignmentsMap.entrySet()) {
            com.bustrackpro.modal.user.Users owner = entry.getKey();
            List<ScheduleAssignment> ownerAssignments = entry.getValue();

            StringBuilder sb = new StringBuilder();
            sb.append(String.format("Generated Daily Timetable for %s\n\n", targetDate));

            for (ScheduleAssignment sa : ownerAssignments) {
                Trip trip = sa.getTrip();
                BusRoute route = (trip != null && trip.getTimetable() != null) ? trip.getTimetable().getBusRoute() : null;
                String startLoc = route != null && route.getStartLocation() != null ? route.getStartLocation() : "Start";
                String endLoc = route != null && route.getEndLocation() != null ? route.getEndLocation() : "End";

                com.bustrackpro.modal.schedule.TripDirection dir = (trip != null && trip.getDirection() != null) ? trip.getDirection() : com.bustrackpro.modal.schedule.TripDirection.MAIN_WAY;
                boolean isMain = dir == com.bustrackpro.modal.schedule.TripDirection.MAIN_WAY;
                String depLoc = isMain ? startLoc : endLoc;
                String arrLoc = isMain ? endLoc : startLoc;
                String dirLabel = depLoc + " to " + arrLoc;

                sb.append(String.format("• Bus: %s | Trip: %s - %s (%s)\n  Dep Stand: %s | Arr Stand: %s\n  Driver: %s\n\n",
                        sa.getBus().getBusNumber(),
                        trip != null ? trip.getDepartureTime() : "N/A",
                        trip != null ? trip.getArrivalTime() : "N/A",
                        dirLabel,
                        depLoc + " Departure",
                        arrLoc + " Arrival",
                        sa.getDriver() != null && sa.getDriver().getUsers() != null ? sa.getDriver().getUsers().getFirstName() + " " + sa.getDriver().getUsers().getLastName() : "Unassigned"
                ));
            }

            try {
                notificationService.createNotification(
                        "Daily Timetable Dispatch: " + targetDate,
                        sb.toString().trim(),
                        com.bustrackpro.modal.NotificationCategory.USER_UPDATE,
                        "SYSTEM_AUTO_SCHEDULER",
                        java.util.Collections.singletonList(owner.getId())
                );
                log.info("Dispatched daily timetable notification to Bus Owner ID: {} for date: {}", owner.getId(), targetDate);
            } catch (Exception e) {
                log.error("Failed to dispatch daily timetable to owner {}: {}", owner.getId(), e.getMessage());
            }
        }
    }

    private void assignTrip(Trip trip, LocalDate targetDate, List<Bus> allBuses, List<BusDriver> allDrivers,
                            Map<Integer, LocalTime> busAvailability,
                            Map<Integer, LocalTime> driverAvailability,
                            Map<Integer, Integer> driverDailyMinutes) {
        
        LocalTime departureTime = trip.getDepartureTime();
        int estimatedDuration = trip.getEstimatedDurationMinutes() != null ? trip.getEstimatedDurationMinutes() : 60;

        // 1. Find all available buses
        List<Bus> availableBuses = new ArrayList<>();
        for (Bus bus : allBuses) {
            LocalTime availableFrom = busAvailability.getOrDefault(bus.getId(), LocalTime.MIDNIGHT);
            if (!availableFrom.isAfter(departureTime)) {
                availableBuses.add(bus);
            }
        }

        // 2. Find all available drivers (respecting rest time and daily limits)
        List<BusDriver> availableDrivers = new ArrayList<>();
        for (BusDriver driver : allDrivers) {
            LocalTime availableFrom = driverAvailability.getOrDefault(driver.getId(), LocalTime.MIDNIGHT);
            int dailyMinutes = driverDailyMinutes.getOrDefault(driver.getId(), 0);

            if (!availableFrom.isAfter(departureTime) && (dailyMinutes + estimatedDuration <= MAX_DAILY_DRIVING_MINUTES)) {
                availableDrivers.add(driver);
            }
        }

        ScheduleAssignment assignment = new ScheduleAssignment();
        assignment.setTrip(trip);
        assignment.setAssignmentDate(targetDate);
        assignment.setNotes("Auto-generated");

        if (availableBuses.isEmpty() || availableDrivers.isEmpty()) {
            log.warn("Shortage of resources for trip {} at {}. Buses available: {}, Drivers available: {}", 
                     trip.getId(), departureTime, availableBuses.size(), availableDrivers.size());
            assignment.setStatus(AssignmentStatus.PENDING); // Mark as pending if no resources
        } else {
            // Randomly select bus and driver
            Bus selectedBus = availableBuses.get(random.nextInt(availableBuses.size()));
            BusDriver selectedDriver = availableDrivers.get(random.nextInt(availableDrivers.size()));

            assignment.setBus(selectedBus);
            assignment.setDriver(selectedDriver);
            assignment.setStatus(AssignmentStatus.SCHEDULED);

            // Calculate next available times
            int buffer = trip.getBufferTimeMinutes() != null ? trip.getBufferTimeMinutes() : 0;
            
            // Handle LocalTime overflow
            LocalTime tripEnd = trip.getArrivalTime().plusMinutes(buffer);
            busAvailability.put(selectedBus.getId(), tripEnd);
            
            LocalTime driverFreeTime = tripEnd.plusMinutes(DRIVER_REST_TIME_MINUTES);
            driverAvailability.put(selectedDriver.getId(), driverFreeTime);
            
            driverDailyMinutes.put(selectedDriver.getId(), driverDailyMinutes.getOrDefault(selectedDriver.getId(), 0) + estimatedDuration);
        }

        assignmentRepository.save(assignment);
    }


}
