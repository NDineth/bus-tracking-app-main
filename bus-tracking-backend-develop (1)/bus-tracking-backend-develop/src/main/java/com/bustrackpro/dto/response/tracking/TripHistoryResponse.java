package com.bustrackpro.dto.response.tracking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for trip history records shown in the trip history page.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripHistoryResponse {
    private Integer id;
    private Integer busId;
    private Long routeId;
    private String registrationNumber;
    private String routeNumber;
    private String tripCode;
    private String tripLabel;
    private String startLocation;
    private String endLocation;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
    private Integer haltsVisited;
    private Integer totalHalts;
    private Integer violationCount;
    private Double distanceCovered;
    private Double avgSpeed;
    private Integer gpsLogCount;
    private Integer delayMinutes;
    private Integer standardDurationMinutes;
    private Integer actualDurationMinutes;
    private Boolean isDelayed;
    private String delayStatus;
    private List<MissedHaltInfo> missedHalts;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MissedHaltInfo {
        private String name;
        private Integer stopOrder;
    }
}
