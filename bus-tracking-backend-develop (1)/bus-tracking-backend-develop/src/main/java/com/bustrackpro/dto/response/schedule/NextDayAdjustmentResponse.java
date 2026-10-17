package com.bustrackpro.dto.response.schedule;

import java.util.List;
import java.util.Map;

public record NextDayAdjustmentResponse(
        List<AdjustedAssignment> adjustedPlan,
        List<RemovedAssignment> removedAssignments,
        Map<String, Object> slotSummary,
        List<String> alerts,
        Map<String, Object> compliance
) {
    public record AdjustedAssignment(
            String busNo,
            String slotName,
            String status, // "UNCHANGED", "ADJUSTED", "REASSIGNED_FROM_NIGHT"
            Integer bufferTimeMinutes
    ) {}

    public record RemovedAssignment(
            String busNo,
            String originalSlotName,
            String reason
    ) {}
}
