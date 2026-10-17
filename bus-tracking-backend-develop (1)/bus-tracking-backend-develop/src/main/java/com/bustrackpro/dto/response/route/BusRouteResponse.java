package com.bustrackpro.dto.response.route;

import com.bustrackpro.modal.route.BusRoute;
import com.bustrackpro.modal.route.BusRouteHalt;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for sending bus route responses.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BusRouteResponse {
    private BusRoute busRoute;
    private List<BusRouteHalt> busRouteHalts;
}
