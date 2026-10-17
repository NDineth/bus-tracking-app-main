package com.bustrackpro.dto.request.route;

import lombok.Data;

/**
 * DTO for receiving bus halt requests.
 */
@Data
public class BusHaltRequest {
    private String name;
    private Double latitude;
    private Double longitude;
    private int stopOrder;
}
