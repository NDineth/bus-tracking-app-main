package com.bustrackpro.repository.schedule;

import com.bustrackpro.modal.schedule.Timetable;
import com.bustrackpro.modal.schedule.Trip;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Trip repository.
 */
@Repository
public interface TripRepository extends JpaRepository<Trip, Integer> {

    /**
     * Find all trips for a timetable.
     *
     * @param timetable - the timetable
     * @return list of trips
     */
    List<Trip> findByTimetable(Timetable timetable);

    /**
     * Find all trips for a timetable ordered by departure time.
     *
     * @param timetable - the timetable
     * @return list of trips sorted by departure time ascending
     */
    List<Trip> findByTimetableOrderByDepartureTimeAsc(Timetable timetable);

    /**
     * Delete all trips belonging to a timetable.
     *
     * @param timetable - the timetable
     */
    void deleteByTimetable(Timetable timetable);
}
