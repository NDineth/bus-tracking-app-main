package com.bustrackpro.service.schedule;

import com.bustrackpro.dto.request.schedule.TripRequest;
import com.bustrackpro.dto.response.schedule.TripResponse;
import com.bustrackpro.modal.schedule.Timetable;
import com.bustrackpro.modal.schedule.Trip;
import com.bustrackpro.modal.schedule.TripStatus;
import com.bustrackpro.repository.schedule.ScheduleAssignmentRepository;
import com.bustrackpro.repository.schedule.TimetableRepository;
import com.bustrackpro.repository.schedule.TripRepository;
import java.time.Duration;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for trip CRUD operations within timetables.
 */
@Service
@RequiredArgsConstructor
public class TripService {

    private final TripRepository tripRepository;
    private final TimetableRepository timetableRepository;
    private final ScheduleAssignmentRepository assignmentRepository;

    /**
     * Add a trip to a timetable.
     *
     * @param request - the trip request
     * @return the created trip response
     */
    public TripResponse createTrip(TripRequest request) {
        Timetable timetable = timetableRepository.findById(
                request.getTimetableId())
                .orElseThrow(() -> new RuntimeException(
                        "Timetable not found with id: "
                        + request.getTimetableId()));

        LocalTime departureTime = LocalTime.parse(request.getDepartureTime());
        LocalTime arrivalTime = LocalTime.parse(request.getArrivalTime());

        validateTimes(departureTime, arrivalTime);

        Trip trip = new Trip();
        trip.setTimetable(timetable);
        trip.setDepartureTime(departureTime);
        trip.setArrivalTime(arrivalTime);
        trip.setBufferTimeMinutes(request.getBufferTimeMinutes() != null
                ? request.getBufferTimeMinutes() : 0);
        trip.setEstimatedDurationMinutes(calculateDuration(
                departureTime, arrivalTime));
        trip.setTripCode(request.getTripCode());
        trip.setTripLabel(request.getTripLabel());
        
        if (request.getDirection() != null && !request.getDirection().isBlank()) {
            try {
                trip.setDirection(com.bustrackpro.modal.schedule.TripDirection.valueOf(request.getDirection()));
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Invalid trip direction: " + request.getDirection());
            }
        }
        
        trip.setStatus(TripStatus.ACTIVE);

        trip = tripRepository.save(trip);
        return mapToResponse(trip);
    }

    /**
     * Get all trips for a timetable.
     *
     * @param timetableId - the timetable id
     * @return list of trip responses sorted by departure time
     */
    public List<TripResponse> getTripsByTimetable(Integer timetableId) {
        Timetable timetable = timetableRepository.findById(timetableId)
                .orElseThrow(() -> new RuntimeException(
                        "Timetable not found with id: " + timetableId));

        return tripRepository.findByTimetableOrderByDepartureTimeAsc(timetable)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get a trip by id.
     *
     * @param id - the trip id
     * @return the trip response
     */
    public TripResponse getTripById(Integer id) {
        Trip trip = findTripOrThrow(id);
        return mapToResponse(trip);
    }

    /**
     * Update a trip.
     *
     * @param id      - the trip id
     * @param request - the updated request
     * @return the updated trip response
     */
    public TripResponse updateTrip(Integer id, TripRequest request) {
        Trip trip = findTripOrThrow(id);

        LocalTime departureTime = LocalTime.parse(request.getDepartureTime());
        LocalTime arrivalTime = LocalTime.parse(request.getArrivalTime());

        validateTimes(departureTime, arrivalTime);

        if (request.getTimetableId() != null
                && !request.getTimetableId().equals(
                        trip.getTimetable().getId())) {
            Timetable newTimetable = timetableRepository.findById(
                    request.getTimetableId())
                    .orElseThrow(() -> new RuntimeException(
                            "Timetable not found with id: "
                            + request.getTimetableId()));
            trip.setTimetable(newTimetable);
        }

        trip.setDepartureTime(departureTime);
        trip.setArrivalTime(arrivalTime);
        trip.setBufferTimeMinutes(request.getBufferTimeMinutes() != null
                ? request.getBufferTimeMinutes() : trip.getBufferTimeMinutes());
        trip.setEstimatedDurationMinutes(calculateDuration(
                departureTime, arrivalTime));
        trip.setTripCode(request.getTripCode() != null
                ? request.getTripCode() : trip.getTripCode());
        trip.setTripLabel(request.getTripLabel());
        
        if (request.getDirection() != null && !request.getDirection().isBlank()) {
            try {
                trip.setDirection(com.bustrackpro.modal.schedule.TripDirection.valueOf(request.getDirection()));
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Invalid trip direction: " + request.getDirection());
            }
        }

        trip = tripRepository.save(trip);
        return mapToResponse(trip);
    }

    /**
     * Delete a trip and its assignments.
     *
     * @param id - the trip id
     */
    @Transactional
    public void deleteTrip(Integer id) {
        Trip trip = findTripOrThrow(id);
        assignmentRepository.deleteByTrip(trip);
        tripRepository.delete(trip);
    }

    private Trip findTripOrThrow(Integer id) {
        return tripRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                        "Trip not found with id: " + id));
    }

    private void validateTimes(
            LocalTime departureTime, LocalTime arrivalTime) {
        // Allowing midnight crossing: arrival can be before departure.
        // We just need to make sure they aren't exactly the same.
        if (departureTime.equals(arrivalTime)) {
            throw new RuntimeException(
                    "Departure and arrival times cannot be the same");
        }
    }

    private Integer calculateDuration(
            LocalTime departureTime, LocalTime arrivalTime) {
        Duration duration = Duration.between(departureTime, arrivalTime);
        if (duration.isNegative()) {
            duration = duration.plusDays(1);
        }
        return (int) duration.toMinutes();
    }

    private TripResponse mapToResponse(Trip trip) {
        return TripResponse.builder()
                .id(trip.getId())
                .timetableId(trip.getTimetable().getId())
                .timetableName(trip.getTimetable().getName())
                .departureTime(trip.getDepartureTime().toString())
                .arrivalTime(trip.getArrivalTime().toString())
                .bufferTimeMinutes(trip.getBufferTimeMinutes())
                .estimatedDurationMinutes(trip.getEstimatedDurationMinutes())
                .tripCode(trip.getTripCode())
                .tripLabel(trip.getTripLabel())
                .direction(trip.getDirection() != null ? trip.getDirection().name() : null)
                .status(trip.getStatus().name())
                .build();
    }
}
