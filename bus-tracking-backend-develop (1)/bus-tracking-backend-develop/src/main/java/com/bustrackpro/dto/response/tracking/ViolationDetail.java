package com.bustrackpro.dto.response.tracking;

import lombok.Builder;
import lombok.Data;

/**
 * DTO representing a single violation detail in a tracking response.
 */
@Data
@Builder
public class ViolationDetail {

    private String type;

    private String description;

    private Double latitude;

    private Double longitude;

    private String detectedAt;
}
