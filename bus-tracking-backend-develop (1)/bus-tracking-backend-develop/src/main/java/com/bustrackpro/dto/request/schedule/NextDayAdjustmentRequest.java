package com.bustrackpro.dto.request.schedule;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record NextDayAdjustmentRequest(
        String planningMode,
        LocalDate date,
        int confirmedRunningBusCount,
        List<String> unavailableBuses,
        Map<String, Object> policy
) {
}
