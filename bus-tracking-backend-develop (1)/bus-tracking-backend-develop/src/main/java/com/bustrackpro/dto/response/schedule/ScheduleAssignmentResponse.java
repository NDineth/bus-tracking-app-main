package com.bustrackpro.dto.response.schedule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for sending schedule assignment responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleAssignmentResponse {

    private Integer id;
    private Integer tripId;
    private String tripLabel;
    private String departureTime;
    private String arrivalTime;
    private Integer busId;
    private String busNumber;
    private Integer driverId;
    private String driverName;
    private String assignmentDate;
    private String status;
    private String notes;
    private Integer bufferTimeMinutes;
    private java.math.BigDecimal permitFee;
    private java.math.BigDecimal offPeakCommission;
    private Boolean isOffPeak;
    private Long routeId;
    private String routeNumber;
    private Integer delayMinutes;
    private java.math.BigDecimal fineAmount;
    private Integer penaltyLevel;
    private Double actualDistance;
}
