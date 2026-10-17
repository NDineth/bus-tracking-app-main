package com.bustrackpro.dto.response.bus;

import com.bustrackpro.dto.response.user.UserResponse;
import lombok.Builder;
import lombok.Data;
import java.util.List;

/**
 * DTO for sending bus responses.
 */
@Data
@Builder
public class BusResponse {
    private Integer id;
    private String busNumber;
    private Integer capacity;
    private String model;
    private UserResponse driver;
    private UserResponse owner;
    private Integer routeId;
    private String route;
    private String nextTurn;
    private List<String> turns;
    private String status;
    private String availabilityStatus;
    private String unavailabilityReason;
}
