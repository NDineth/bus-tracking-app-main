package com.bustrackpro.dto.request.tracking;

import com.bustrackpro.modal.tracking.DrivingBehaviorType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DrivingAlertRequest {
    private Integer busId;
    private DrivingBehaviorType behavior;
    private Double latitude;
    private Double longitude;
    private Long timestamp;
}
