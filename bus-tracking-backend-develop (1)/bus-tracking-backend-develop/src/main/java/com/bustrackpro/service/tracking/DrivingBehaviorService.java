package com.bustrackpro.service.tracking;

import com.bustrackpro.dto.request.tracking.DrivingAlertRequest;
import com.bustrackpro.dto.request.tracking.AccelerationReadingDto;
import java.util.List;

public interface DrivingBehaviorService {
    void processDrivingAlert(DrivingAlertRequest alert);
    void analyzeDrivingBehavior(Integer busId, List<AccelerationReadingDto> acceleration, Double latitude, Double longitude);
}
