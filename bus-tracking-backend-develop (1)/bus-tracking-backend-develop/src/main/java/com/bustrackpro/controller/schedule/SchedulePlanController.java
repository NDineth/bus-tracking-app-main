package com.bustrackpro.controller.schedule;

import com.bustrackpro.dto.request.schedule.MonthlyPlanRequest;
import com.bustrackpro.dto.request.schedule.NextDayAdjustmentRequest;
import com.bustrackpro.dto.response.schedule.MonthlyPlanResponse;
import com.bustrackpro.dto.response.schedule.NextDayAdjustmentResponse;
import com.bustrackpro.service.schedule.SchedulePlanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/schedules")
public class SchedulePlanController {

    private final SchedulePlanService schedulePlanService;

    @Autowired
    public SchedulePlanController(SchedulePlanService schedulePlanService) {
        this.schedulePlanService = schedulePlanService;
    }

    @PostMapping("/monthly-base-plan")
    public ResponseEntity<MonthlyPlanResponse> generateMonthlyBasePlan(@RequestBody MonthlyPlanRequest request) {
        if (!"MONTHLY_BASE_PLAN".equals(request.planningMode())) {
            return ResponseEntity.badRequest().build();
        }
        MonthlyPlanResponse response = schedulePlanService.generateMonthlyPlan(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/next-day-adjustment")
    public ResponseEntity<NextDayAdjustmentResponse> generateNextDayAdjustment(@RequestBody NextDayAdjustmentRequest request) {
        if (!"NEXT_DAY_ADJUSTMENT".equals(request.planningMode())) {
            return ResponseEntity.badRequest().build();
        }
        NextDayAdjustmentResponse response = schedulePlanService.generateNextDayAdjustment(request);
        return ResponseEntity.ok(response);
    }
}
