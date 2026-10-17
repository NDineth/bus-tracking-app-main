package com.bustrackpro.dto.response.schedule;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record MonthlyPlanResponse(
        Map<String, Object> basePlanSummary,
        List<MonthlyPlanDay> monthlyPlan
) {
    public record MonthlyPlanDay(
            LocalDate date,
            List<Assignment> assignments
    ) {}

    public record Assignment(
            String busNo,
            String tripCode,
            LocalDateTime departureTime,
            LocalDateTime estimatedArrivalTime,
            Integer bufferTimeMinutes
    ) {}
}
