package com.bustrackpro.service.schedule;

import com.bustrackpro.dto.request.schedule.MonthlyPlanRequest;
import com.bustrackpro.dto.request.schedule.NextDayAdjustmentRequest;
import com.bustrackpro.dto.response.schedule.MonthlyPlanResponse;
import com.bustrackpro.dto.response.schedule.NextDayAdjustmentResponse;

public interface SchedulePlanService {
    MonthlyPlanResponse generateMonthlyPlan(MonthlyPlanRequest request);
    NextDayAdjustmentResponse generateNextDayAdjustment(NextDayAdjustmentRequest request);
}
