package com.bustrackpro.dto.request.bus;

import lombok.Data;

/**
 * DTO for receiving bus requests.
 */
@Data
public class BusRequest {
    private String busNumber;
    private Integer capacity;
    private String model;
    private Integer driverId;
    private Integer ownerId;
    private Integer routeId;
}
