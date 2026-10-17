package com.bustrackpro.service.schedule;

import com.bustrackpro.dto.request.schedule.TimetableRequest;
import com.bustrackpro.dto.response.schedule.TimetableResponse;
import com.bustrackpro.modal.route.BusRoute;
import com.bustrackpro.modal.schedule.DayType;
import com.bustrackpro.modal.schedule.ScheduleType;
import com.bustrackpro.modal.schedule.Timetable;
import com.bustrackpro.modal.schedule.Trip;
import com.bustrackpro.modal.schedule.TripStatus;
import com.bustrackpro.repository.BusRouteRepository;
import com.bustrackpro.repository.schedule.TimetableRepository;
import com.bustrackpro.repository.schedule.TripRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for timetable CRUD operations.
 */
@Service
@RequiredArgsConstructor
public class TimetableService {

    private final TimetableRepository timetableRepository;
    private final TripRepository tripRepository;
    private final BusRouteRepository busRouteRepository;

    /**
     * Create a new timetable.
     *
     * @param request - the timetable request
     * @return the created timetable response
     */
    public TimetableResponse createTimetable(TimetableRequest request) {
        BusRoute route = busRouteRepository.findById(
                request.getRouteId().intValue())
                .orElseThrow(() -> new RuntimeException(
                        "Route not found with id: " + request.getRouteId()));

        LocalDate activeDate = LocalDate.parse(request.getActiveDate());
        LocalDate expirationDate = LocalDate.parse(request.getExpirationDate());
        DayType dayType = DayType.valueOf(request.getDayType());
        ScheduleType scheduleType = ScheduleType.valueOf(request.getScheduleType());

        validateDates(activeDate, expirationDate);
        validateNoOverlap(route.getId(), dayType, scheduleType,
                activeDate, expirationDate, null);

        Timetable timetable = new Timetable();
        timetable.setName(request.getName());
        timetable.setBusRoute(route);
        timetable.setDayType(dayType);
        timetable.setScheduleType(ScheduleType.valueOf(request.getScheduleType()));
        timetable.setActiveDate(activeDate);
        timetable.setExpirationDate(expirationDate);

        Timetable savedTimetable = timetableRepository.save(timetable);

        int tripCount = 0;
        if (request.getDuplicateFromTimetableId() != null) {
            Timetable sourceTimetable = findTimetableOrThrow(request.getDuplicateFromTimetableId());
            List<Trip> originalTrips = tripRepository.findByTimetable(sourceTimetable);
            List<Trip> newTrips = originalTrips.stream().map(original -> {
                Trip t = new Trip();
                t.setTimetable(savedTimetable);
                t.setDepartureTime(original.getDepartureTime());
                t.setArrivalTime(original.getArrivalTime());
                t.setBufferTimeMinutes(original.getBufferTimeMinutes());
                t.setEstimatedDurationMinutes(original.getEstimatedDurationMinutes());
                t.setTripCode(original.getTripCode());
                t.setTripLabel(original.getTripLabel());
                t.setStatus(TripStatus.ACTIVE);
                t.setDirection(original.getDirection());
                return t;
            }).collect(Collectors.toList());
            tripRepository.saveAll(newTrips);
            tripCount = newTrips.size();
        }

        return mapToResponse(savedTimetable, tripCount);
    }

    /**
     * Get all timetables.
     *
     * @return list of timetable responses
     */
    public List<TimetableResponse> getAllTimetables() {
        return timetableRepository.findAll().stream()
                .map(t -> mapToResponse(t,
                        tripRepository.findByTimetable(t).size()))
                .collect(Collectors.toList());
    }

    /**
     * Get timetable by id.
     *
     * @param id - the timetable id
     * @return the timetable response
     */
    public TimetableResponse getTimetableById(Integer id) {
        Timetable timetable = findTimetableOrThrow(id);
        int tripCount = tripRepository.findByTimetable(timetable).size();
        return mapToResponse(timetable, tripCount);
    }

    /**
     * Get timetables for a specific route.
     *
     * @param routeId - the route id
     * @return list of timetable responses
     */
    public List<TimetableResponse> getTimetablesByRoute(Integer routeId) {
        BusRoute route = busRouteRepository.findById(routeId)
                .orElseThrow(() -> new RuntimeException(
                        "Route not found with id: " + routeId));

        return timetableRepository.findByBusRoute(route).stream()
                .map(t -> mapToResponse(t,
                        tripRepository.findByTimetable(t).size()))
                .collect(Collectors.toList());
    }

    /**
     * Update an existing timetable.
     *
     * @param id      - the timetable id
     * @param request - the updated request
     * @return the updated timetable response
     */
    public TimetableResponse updateTimetable(
            Integer id, TimetableRequest request) {
        Timetable timetable = findTimetableOrThrow(id);

        BusRoute route = busRouteRepository.findById(
                request.getRouteId().intValue())
                .orElseThrow(() -> new RuntimeException(
                        "Route not found with id: " + request.getRouteId()));

        LocalDate activeDate = LocalDate.parse(request.getActiveDate());
        LocalDate expirationDate = LocalDate.parse(request.getExpirationDate());
        DayType dayType = DayType.valueOf(request.getDayType());
        ScheduleType scheduleType = ScheduleType.valueOf(request.getScheduleType());

        validateDates(activeDate, expirationDate);
        validateNoOverlap(route.getId(), dayType, scheduleType,
                activeDate, expirationDate, id);

        timetable.setName(request.getName());
        timetable.setBusRoute(route);
        timetable.setDayType(dayType);
        timetable.setScheduleType(ScheduleType.valueOf(request.getScheduleType()));
        timetable.setActiveDate(activeDate);
        timetable.setExpirationDate(expirationDate);

        timetable = timetableRepository.save(timetable);
        int tripCount = tripRepository.findByTimetable(timetable).size();
        return mapToResponse(timetable, tripCount);
    }

    /**
     * Delete a timetable and its associated trips.
     *
     * @param id - the timetable id
     */
    @Transactional
    public void deleteTimetable(Integer id) {
        Timetable timetable = findTimetableOrThrow(id);
        tripRepository.deleteByTimetable(timetable);
        timetableRepository.delete(timetable);
    }

    private Timetable findTimetableOrThrow(Integer id) {
        return timetableRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                        "Timetable not found with id: " + id));
    }

    private void validateDates(
            LocalDate activeDate, LocalDate expirationDate) {
        if (expirationDate.isBefore(activeDate)) {
            throw new RuntimeException(
                    "Expiration date must be after active date");
        }
    }

    private void validateNoOverlap(long routeId, DayType dayType, ScheduleType scheduleType,
            LocalDate activeDate, LocalDate expirationDate,
            Integer excludeId) {
        long count = timetableRepository.countOverlapping(
                routeId, dayType, scheduleType, activeDate, expirationDate, excludeId);
        if (count > 0) {
            throw new RuntimeException(
                    "An overlapping timetable already exists for this "
                    + "route, day type, and schedule type in the specified date range");
        }
    }

    private TimetableResponse mapToResponse(
            Timetable timetable, int tripCount) {
        BusRoute route = timetable.getBusRoute();
        TimetableResponse response = new TimetableResponse();
        response.setId(timetable.getId());
        response.setName(timetable.getName());
        response.setRouteId(route.getId());
        response.setRouteNumber(route.getRouteNumber());
        response.setRouteStartLocation(route.getStartLocation());
        response.setRouteEndLocation(route.getEndLocation());
        response.setDayType(timetable.getDayType().name());
        response.setScheduleType(timetable.getScheduleType().name());
        response.setActiveDate(timetable.getActiveDate().toString());
        response.setExpirationDate(timetable.getExpirationDate().toString());
        response.setTripCount(tripCount);
        response.setCreatedAt(timetable.getCreatedAt() != null
                ? timetable.getCreatedAt().toString() : null);
        return response;
    }
}
