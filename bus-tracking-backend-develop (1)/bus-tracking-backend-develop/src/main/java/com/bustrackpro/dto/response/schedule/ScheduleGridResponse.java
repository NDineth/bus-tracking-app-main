package com.bustrackpro.dto.response.schedule;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for the scheduler grid view showing trips with their assignments.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleGridResponse {

    private Integer timetableId;
    private String timetableName;
    private String routeNumber;
    private String startLocation;
    private String endLocation;
    private String dayType;
    private String activeDate;
    private String expirationDate;
    private List<TripWithAssignment> trips;

    /**
     * A trip along with its assignment for a given date.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TripWithAssignment {
        private Integer id;
        private String tripLabel;
        private String departureTime;
        private String arrivalTime;
        private Integer bufferTimeMinutes;
        private String tripStatus;
        private String direction;
        private String departureLocation;
        private String arrivalLocation;
        private String departureStand;
        private String arrivalStand;
        private String directionLabel;
        private List<ScheduleAssignmentResponse> assignments;
    }
}
