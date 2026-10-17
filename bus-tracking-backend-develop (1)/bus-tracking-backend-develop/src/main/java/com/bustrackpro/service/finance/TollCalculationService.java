package com.bustrackpro.service.finance;

import com.bustrackpro.dto.request.finance.DailySettlementRequest;
import com.bustrackpro.dto.request.finance.RateCardUpdateRequest;
import com.bustrackpro.dto.response.finance.BusFinanceSummaryItem;
import com.bustrackpro.dto.response.finance.DailyNetSummaryResponse;
import com.bustrackpro.dto.response.finance.DailySettlementResponse;
import com.bustrackpro.dto.response.finance.RateCardResponse;
import java.time.LocalDate;
import java.util.List;

/**
 * Service for finance calculations, rate card management, and summary reporting.
 */
public interface TollCalculationService {

    /**
     * Calculates the daily settlement for a bus based on its trips.
     *
     * @param request The daily settlement request containing bus ID and trips.
     * @return The calculated daily settlement response.
     */
    DailySettlementResponse calculateDailySettlement(DailySettlementRequest request);

    /**
     * Get daily net summary of permit fees and commissions for a bus.
     *
     * @param busId the bus ID
     * @param date the target date string (yyyy-MM-dd)
     * @return the daily net summary response
     */
    DailyNetSummaryResponse getDailyNetSummary(Integer busId, String date);

    /**
     * Fetch the current (latest) rate card from the database.
     * If no rate card exists, returns the system defaults.
     *
     * @return the current rate card
     */
    RateCardResponse getRateCard();

    /**
     * Update the rate card (Admin only). Inserts a new rate card record.
     *
     * @param request     the new rates
     * @param updatedByEmail the email of the admin making the change
     * @return the saved rate card
     */
    RateCardResponse updateRateCard(RateCardUpdateRequest request, String updatedByEmail);

    /**
     * Get aggregated finance summary per bus for a date range.
     * BusOwner callers should pass their userId to scope results to their fleet.
     *
     * @param busId     optional bus filter (null = all buses)
     * @param from      start date
     * @param to        end date
     * @param ownerId   owner's user ID for BusOwner scoping (null = Admin, returns all)
     * @return list of per-bus finance summaries
     */
    List<BusFinanceSummaryItem> getFinanceSummary(Integer busId, LocalDate from,
            LocalDate to, Integer ownerId);
}
