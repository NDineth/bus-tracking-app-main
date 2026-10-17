package com.bustrackpro.dto.response.schedule;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for sending available buses and drivers for a time slot.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvailableResourcesResponse {

    private List<BusSummary> availableBuses;
    private List<DriverSummary> availableDrivers;

    /**
     * Summary of an available bus.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BusSummary {
        private Integer id;
        private String busNumber;
        private String model;
        private Integer capacity;
        private Integer driverId;
    }

    /**
     * Summary of an available driver.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DriverSummary {
        private Integer id;
        private String licenseNumber;
        private String driverName;
    }
}
