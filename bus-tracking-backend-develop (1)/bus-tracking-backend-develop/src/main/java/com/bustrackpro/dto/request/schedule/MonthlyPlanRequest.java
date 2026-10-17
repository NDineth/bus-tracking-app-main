package com.bustrackpro.dto.request.schedule;

import java.util.List;
import java.util.Map;

public record MonthlyPlanRequest(
        String planningMode,
        String month,
        Integer routeId,
        List<TimeSlotDto> timeSlots,
        Map<String, Object> policy
) {
    public record TimeSlotDto(String id, String name, String startTime, String endTime, String category, int priority) {}
}
