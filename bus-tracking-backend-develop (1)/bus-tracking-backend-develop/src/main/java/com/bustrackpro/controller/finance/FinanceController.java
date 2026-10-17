package com.bustrackpro.controller.finance;

import com.bustrackpro.config.jwt.UserDetailsImpl;
import com.bustrackpro.dto.request.finance.DailySettlementRequest;
import com.bustrackpro.dto.request.finance.RateCardUpdateRequest;
import com.bustrackpro.dto.response.finance.BusFinanceSummaryItem;
import com.bustrackpro.dto.response.finance.DailySettlementResponse;
import com.bustrackpro.dto.response.finance.RateCardResponse;
import com.bustrackpro.modal.user.UserRole;
import com.bustrackpro.repository.user.UserRepository;
import com.bustrackpro.service.finance.TollCalculationService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for finance-related operations: rate card management and summary reporting.
 */
@RestController
@RequestMapping("/finance")
@RequiredArgsConstructor
public class FinanceController {

    private final TollCalculationService tollCalculationService;
    private final UserRepository userRepository;

    // ── Existing endpoint (unchanged) ────────────────────────────────────────

    /**
     * Calculates the daily settlement for a bus.
     *
     * @param request The daily settlement request containing bus ID and trips.
     * @return The calculated daily settlement response.
     */
    @PostMapping("/calculate-settlement")
    public ResponseEntity<DailySettlementResponse> calculateDailySettlement(
            @RequestBody DailySettlementRequest request) {
        DailySettlementResponse response = tollCalculationService.calculateDailySettlement(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Get daily net permit and commission summary for a bus.
     *
     * @param busId the bus ID
     * @param date  the date query parameter (yyyy-MM-dd)
     * @return the daily net summary response
     */
    @GetMapping("/buses/{busId}/daily-net")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'BUS_OWNER', 'BUS_DRIVER')")
    public ResponseEntity<com.bustrackpro.dto.response.finance.DailyNetSummaryResponse> getDailyNetSummary(
            @PathVariable Integer busId,
            @RequestParam String date) {
        return ResponseEntity.ok(tollCalculationService.getDailyNetSummary(busId, date));
    }

    // ── Rate Card endpoints ────────────────────────────────────────────────

    /**
     * GET /finance/rates — returns the current rate card.
     * Accessible by ADMIN and BUS_OWNER.
     *
     * @return the current rate card
     */
    @GetMapping("/rates")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'BUS_OWNER')")
    public ResponseEntity<RateCardResponse> getRates() {
        return ResponseEntity.ok(tollCalculationService.getRateCard());
    }

    /**
     * PUT /finance/rates — update the rate card (Admin only).
     *
     * @param request      new rate values
     * @param userDetails  the authenticated user (extracted from JWT)
     * @return the saved rate card
     */
    @PutMapping("/rates")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<RateCardResponse> updateRates(
            @Valid @RequestBody RateCardUpdateRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        String adminEmail = userDetails != null ? userDetails.getUsername() : "system";
        return ResponseEntity.ok(tollCalculationService.updateRateCard(request, adminEmail));
    }

    // ── Finance Summary endpoint ───────────────────────────────────────────

    /**
     * GET /finance/summary — aggregated per-bus permit fee and commission summary.
     *
     * <ul>
     *   <li>ADMIN: sees all buses (ownerId param ignored).</li>
     *   <li>BUS_OWNER: sees only buses they own (ownerId is resolved from JWT automatically).</li>
     * </ul>
     *
     * @param busId       optional bus ID filter
     * @param from        start date (yyyy-MM-dd), defaults to 30 days ago
     * @param to          end date (yyyy-MM-dd), defaults to today
     * @param userDetails authenticated user from JWT
     * @return list of per-bus finance summaries
     */
    @GetMapping("/summary")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'BUS_OWNER')")
    public ResponseEntity<List<BusFinanceSummaryItem>> getFinanceSummary(
            @RequestParam(required = false) Integer busId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        // Default date range: last 30 days
        LocalDate effectiveTo = to != null ? to : LocalDate.now();
        LocalDate effectiveFrom = from != null ? from : effectiveTo.minusDays(30);

        // Resolve owner scope: BUS_OWNER sees only their buses
        Integer ownerId = null;
        if (userDetails != null) {
            boolean isOwner = userRepository.findById(userDetails.getId())
                    .map(u -> u.getRole() == UserRole.BUS_OWNER)
                    .orElse(false);
            if (isOwner) {
                ownerId = userDetails.getId();
            }
        }

        List<BusFinanceSummaryItem> summary = tollCalculationService.getFinanceSummary(
                busId, effectiveFrom, effectiveTo, ownerId);

        return ResponseEntity.ok(summary);
    }
}
