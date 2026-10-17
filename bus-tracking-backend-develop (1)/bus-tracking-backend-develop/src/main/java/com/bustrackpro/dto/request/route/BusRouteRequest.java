package com.bustrackpro.dto.request.route;

import java.util.List;
import lombok.Data;

/**
 * DTO for receiving bus route requests.
 */
@Data
public class BusRouteRequest {
    private String routeNumber;
    private String startLocation;
    private String endLocation;
    private List<BusHaltRequest> halts;
    private Double distance;
}
