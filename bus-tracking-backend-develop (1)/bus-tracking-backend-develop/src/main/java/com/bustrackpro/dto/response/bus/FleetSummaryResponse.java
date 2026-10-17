package com.bustrackpro.dto.response.bus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for sending active fleet count and bus availability summary.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FleetSummaryResponse {
    private int totalFleetCount;
    private int activeFleetCount;
    private int unavailableBusCount;
    private int pendingRequestCount;
    private String date;
    private List<UnavailableBusDetail> unavailableBuses;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UnavailableBusDetail {
        private Integer busId;
        private String busNumber;
        private String reason;
        private String approvalType;
        private String emergencyType;
    }
}
