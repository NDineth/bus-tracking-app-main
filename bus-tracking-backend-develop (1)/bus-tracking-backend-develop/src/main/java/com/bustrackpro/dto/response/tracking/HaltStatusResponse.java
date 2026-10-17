package com.bustrackpro.dto.response.tracking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for halt status in the halt progress indicator.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HaltStatusResponse {
    private String name;
    private Integer stopOrder;
    /** One of: visited, missed, current, upcoming */
    private String status;
    private String visitedAt;
}
