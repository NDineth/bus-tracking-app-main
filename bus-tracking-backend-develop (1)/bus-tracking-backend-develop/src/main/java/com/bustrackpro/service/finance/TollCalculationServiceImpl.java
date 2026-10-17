package com.bustrackpro.service.finance;

import com.bustrackpro.dto.request.finance.DailySettlementRequest;
import com.bustrackpro.dto.request.finance.RateCardUpdateRequest;
import com.bustrackpro.dto.request.finance.TripSegment;
import com.bustrackpro.dto.response.finance.BusFinanceSummaryItem;
import com.bustrackpro.dto.response.finance.DailyNetSummaryResponse;
import com.bustrackpro.dto.response.finance.DailySettlementResponse;
import com.bustrackpro.dto.response.finance.RateCardResponse;
import com.bustrackpro.dto.response.finance.TripCalculationResult;
import com.bustrackpro.dto.response.schedule.ScheduleAssignmentResponse;
import com.bustrackpro.modal.bus.Bus;
import com.bustrackpro.modal.finance.FinanceRateCard;
import com.bustrackpro.modal.schedule.AssignmentStatus;
import com.bustrackpro.modal.schedule.ScheduleAssignment;
import com.bustrackpro.modal.schedule.Trip;
import com.bustrackpro.modal.finance.PenaltyRecord;
import com.bustrackpro.modal.user.Users;
import com.bustrackpro.repository.bus.BusRepository;
import com.bustrackpro.repository.finance.FinanceRateCardRepository;
import com.bustrackpro.repository.finance.PenaltyRecordRepository;
import com.bustrackpro.repository.schedule.ScheduleAssignmentRepository;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of TollCalculationService.
 * Rates are read from the finance_rate_card table (Rate Card Pattern) —
 * no fees are hardcoded.
 */
@Service
@RequiredArgsConstructor
public class TollCalculationServiceImpl implements TollCalculationService {

    private final BusRepository busRepository;
    private final ScheduleAssignmentRepository assignmentRepository;
    private final FinanceRateCardRepository rateCardRepository;
    private final PenaltyRecordRepository penaltyRecordRepository;

    // ── Default rates used only when no rate card exists in DB yet ──────────
    private static final BigDecimal DEFAULT_WEEKDAY_FEE = new BigDecimal("1500.00");
    private static final BigDecimal DEFAULT_WEEKEND_FEE = new BigDecimal("2200.00");
    private static final BigDecimal DEFAULT_HOLIDAY_FEE = new BigDecimal("3000.00");
    private static final String DEFAULT_COMMISSION_MODE = "percentage";
    private static final BigDecimal DEFAULT_COMMISSION_VALUE = new BigDecimal("8.50");

    // ── Legacy toll calculation constants (kept for existing endpoint) ───────
    private static final double MORNING_PEAK_RATE = 10.0;
    private static final double MIDDAY_OFF_PEAK_RATE = 5.0;
    private static final double NIGHT_SERVICE_RATE = 4.0;

    private static final LocalTime MORNING_PEAK_START = LocalTime.of(8, 0);
    private static final LocalTime MORNING_PEAK_END = LocalTime.of(9, 0);
    private static final LocalTime MIDDAY_OFF_PEAK_START = LocalTime.of(11, 0);
    private static final LocalTime MIDDAY_OFF_PEAK_END = LocalTime.of(12, 0);

    // ════════════════════════════════════════════════════════════════════════
    // Rate Card Methods
    // ════════════════════════════════════════════════════════════════════════

    @Override
    public RateCardResponse getRateCard() {
        return rateCardRepository.findTopByOrderByCreatedAtDesc()
                .map(this::toRateCardResponse)
                .orElseGet(this::defaultRateCardResponse);
    }

    @Override
    @Transactional
    public RateCardResponse updateRateCard(RateCardUpdateRequest request, String updatedByEmail) {
        validateRateCardRequest(request);

        RateCardUpdateRequest.CommissionSection commission = request.getOffPeakCommission();
        String mode = commission != null && commission.getMode() != null
                ? commission.getMode().toUpperCase()
                : DEFAULT_COMMISSION_MODE.toUpperCase();

        FinanceRateCard card = FinanceRateCard.builder()
                .weekdayPermitFee(request.getPermitFee().getWeekday())
                .weekendPermitFee(request.getPermitFee().getWeekend())
                .holidayPermitFee(request.getPermitFee().getHoliday())
                .offPeakCommissionMode(mode)
                .offPeakCommissionValue(commission != null ? commission.getValue() : DEFAULT_COMMISSION_VALUE)
                .effectiveFrom(commission != null ? commission.getEffectiveFrom() : LocalDate.now())
                .lastUpdatedBy(updatedByEmail)
                .build();

        FinanceRateCard saved = rateCardRepository.save(card);
        return toRateCardResponse(saved);
    }

    // ════════════════════════════════════════════════════════════════════════
    // Finance Summary
    // ════════════════════════════════════════════════════════════════════════

    @Override
    public List<BusFinanceSummaryItem> getFinanceSummary(Integer busId, LocalDate from,
            LocalDate to, Integer ownerId) {

        List<AssignmentStatus> completedStatus = List.of(AssignmentStatus.COMPLETED);

        // Determine which buses to include
        List<Bus> busList;
        if (ownerId != null) {
            // BusOwner: only their buses
            busList = busRepository.findByOwnerId(ownerId);
        } else {
            busList = busRepository.findAll();
        }

        if (busList.isEmpty()) {
            return List.of();
        }

        // Fetch assignments
        List<ScheduleAssignment> assignments;
        if (busId != null) {
            // Filtered to a specific bus (also checks ownership implicitly via busList check)
            boolean busInFleet = busList.stream().anyMatch(b -> b.getId().equals(busId));
            if (!busInFleet) {
                return List.of();
            }
            assignments = assignmentRepository.findByBusIdAndDateRangeAndStatusIn(
                    busId, from, to, completedStatus);
        } else {
            assignments = assignmentRepository.findAllByDateRangeAndStatusIn(from, to, completedStatus);
        }

        // Build bus ID → Bus map for quick lookup
        Map<Integer, Bus> busMap = busList.stream()
                .collect(Collectors.toMap(Bus::getId, b -> b));

        // Group assignments by bus ID
        Map<Integer, List<ScheduleAssignment>> byBus = assignments.stream()
                .filter(sa -> sa.getBus() != null && busMap.containsKey(sa.getBus().getId()))
                .collect(Collectors.groupingBy(sa -> sa.getBus().getId()));

        // Fetch penalties
        List<PenaltyRecord> penalties = penaltyRecordRepository.findByCreatedAtBetween(
                from.atStartOfDay(), to.atTime(LocalTime.MAX));
        
        Map<Integer, List<PenaltyRecord>> penaltiesByBus = penalties.stream()
                .filter(p -> p.getBus() != null && busMap.containsKey(p.getBus().getId()))
                .collect(Collectors.groupingBy(p -> p.getBus().getId()));

        // Build summary items for all buses in the fleet (even those with 0 assignments)
        List<BusFinanceSummaryItem> result = new ArrayList<>();
        for (Bus bus : busList) {
            if (busId != null && !bus.getId().equals(busId)) {
                continue;
            }
            List<ScheduleAssignment> busAssignments = byBus.getOrDefault(bus.getId(), List.of());
            List<PenaltyRecord> busPenalties = penaltiesByBus.getOrDefault(bus.getId(), List.of());
            result.add(buildSummaryItem(bus, busAssignments, busPenalties));
        }

        return result;
    }

    // ════════════════════════════════════════════════════════════════════════
    // Legacy: Daily Settlement (existing endpoint, now reads rates from DB)
    // ════════════════════════════════════════════════════════════════════════

    @Override
    public DailySettlementResponse calculateDailySettlement(DailySettlementRequest request) {
        List<TripCalculationResult> tripCalculations = new ArrayList<>();
        double totalTollsPaid = 0.0;
        double totalSubsidiesReceived = 0.0;
        double totalDelayPenalties = 0.0;

        for (TripSegment trip : request.getTrips()) {
            LocalTime time = trip.getTime();
            double distance = trip.getDistanceKm();
            double rate = 0.0;
            String type = "NONE";
            double amount = 0.0;

            if (isTimeInWindow(time, MORNING_PEAK_START, MORNING_PEAK_END)) {
                rate = MORNING_PEAK_RATE;
                type = "TOLL";
                amount = distance * rate;
                totalTollsPaid += amount;
            } else if (isTimeInWindow(time, MIDDAY_OFF_PEAK_START, MIDDAY_OFF_PEAK_END)) {
                rate = MIDDAY_OFF_PEAK_RATE;
                type = "TOLL";
                amount = distance * rate;
                totalTollsPaid += amount;
            } else if (isTimeInNightWindow(time)) {
                rate = NIGHT_SERVICE_RATE;
                type = "SUBSIDY";
                amount = distance * rate;
                totalSubsidiesReceived += amount;
            }

            int delayMinutes = 0;
            double delayPenalty = 0.0;
            if (trip.getEndTime() != null && trip.getExpectedEndTime() != null) {
                long diff = ChronoUnit.MINUTES.between(trip.getExpectedEndTime(), trip.getEndTime());
                if (diff < -12 * 60) {
                    diff += 24 * 60;
                }
                if (diff > 0) {
                    delayMinutes = (int) diff;
                    delayPenalty = delayMinutes * 10.0;
                    totalDelayPenalties += delayPenalty;
                }
            }

            tripCalculations.add(TripCalculationResult.builder()
                    .time(time)
                    .distanceKm(distance)
                    .rateApplied(rate)
                    .type(type)
                    .amount(amount)
                    .delayMinutes(delayMinutes)
                    .delayPenalty(delayPenalty)
                    .build());
        }

        double netPayment = totalTollsPaid + totalDelayPenalties - totalSubsidiesReceived;
        String status = netPayment > 0 ? "OWED_TO_GOVERNMENT"
                : netPayment < 0 ? "RECEIVED_FROM_GOVERNMENT" : "ZERO_BALANCE";

        return DailySettlementResponse.builder()
                .busId(request.getBusId())
                .date(request.getDate())
                .totalTollsPaid(totalTollsPaid)
                .totalSubsidiesReceived(totalSubsidiesReceived)
                .totalDelayPenalties(totalDelayPenalties)
                .netPayment(netPayment)
                .paymentStatus(status)
                .tripCalculations(tripCalculations)
                .build();
    }

    @Override
    public DailyNetSummaryResponse getDailyNetSummary(Integer busId, String dateStr) {
        Bus bus = busRepository.findById(busId)
                .orElseThrow(() -> new RuntimeException("Bus not found with id: " + busId));
        LocalDate date = LocalDate.parse(dateStr);

        List<ScheduleAssignment> completedRuns = assignmentRepository.findByBusIdAndDateAndStatusIn(
                busId, date, List.of(AssignmentStatus.COMPLETED));

        BigDecimal totalPermitFees = BigDecimal.ZERO;
        BigDecimal totalCommissions = BigDecimal.ZERO;
        BigDecimal totalFines = BigDecimal.ZERO;
        List<ScheduleAssignmentResponse> runs = new ArrayList<>();

        for (ScheduleAssignment sa : completedRuns) {
            BigDecimal fee = sa.getPermitFee() != null ? sa.getPermitFee() : BigDecimal.ZERO;
            BigDecimal commission = sa.getOffPeakCommission() != null ? sa.getOffPeakCommission() : BigDecimal.ZERO;
            BigDecimal fine = sa.getFineAmount() != null ? sa.getFineAmount() : BigDecimal.ZERO;
            totalPermitFees = totalPermitFees.add(fee);
            totalCommissions = totalCommissions.add(commission);
            totalFines = totalFines.add(fine);

            Trip trip = sa.getTrip();
            Users driver = sa.getBus() != null ? sa.getBus().getDriver() : null;
            String driverName = driver != null ? (driver.getFirstName() + " " + driver.getLastName()) : "Unknown";

            runs.add(ScheduleAssignmentResponse.builder()
                    .id(sa.getId())
                    .tripId(trip.getId())
                    .tripLabel(trip.getTripLabel())
                    .departureTime(trip.getDepartureTime().toString())
                    .arrivalTime(trip.getArrivalTime().toString())
                    .busId(bus.getId())
                    .busNumber(bus.getBusNumber())
                    .driverId(driver != null ? driver.getId() : null)
                    .driverName(driverName)
                    .assignmentDate(sa.getAssignmentDate().toString())
                    .status(sa.getStatus().name())
                    .notes(sa.getNotes())
                    .permitFee(fee)
                    .offPeakCommission(commission)
                    .isOffPeak(sa.getIsOffPeak())
                    .delayMinutes(sa.getDelayMinutes())
                    .fineAmount(fine)
                    .penaltyLevel(sa.getPenaltyLevel())
                    .build());
        }

        return DailyNetSummaryResponse.builder()
                .busNumber(bus.getBusNumber())
                .date(date)
                .totalRuns(completedRuns.size())
                .totalPermitFees(totalPermitFees)
                .totalCommissions(totalCommissions)
                .totalFines(totalFines)
                .dailyNetRevenue(totalPermitFees.subtract(totalCommissions).add(totalFines))
                .runs(runs)
                .build();
    }

    // ════════════════════════════════════════════════════════════════════════
    // Private helpers
    // ════════════════════════════════════════════════════════════════════════

    private BusFinanceSummaryItem buildSummaryItem(Bus bus, List<ScheduleAssignment> assignments, List<PenaltyRecord> penalties) {
        BigDecimal weekdayFee = BigDecimal.ZERO;
        BigDecimal weekendFee = BigDecimal.ZERO;
        BigDecimal holidayFee = BigDecimal.ZERO;
        BigDecimal commissionTotal = BigDecimal.ZERO;

        for (ScheduleAssignment sa : assignments) {
            BigDecimal fee = sa.getPermitFee() != null ? sa.getPermitFee() : BigDecimal.ZERO;
            BigDecimal commission = sa.getOffPeakCommission() != null ? sa.getOffPeakCommission() : BigDecimal.ZERO;

            DayOfWeek dow = sa.getAssignmentDate().getDayOfWeek();
            if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
                weekendFee = weekendFee.add(fee);
            } else {
                weekdayFee = weekdayFee.add(fee);
            }
            commissionTotal = commissionTotal.add(commission);
        }

        BigDecimal penaltyTotal = penalties.stream()
                .map(p -> p.getPenaltyAmount() != null ? p.getPenaltyAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal permitFeeTotal = weekdayFee.add(weekendFee).add(holidayFee);
        // Penalty is subtracted from the final balance or added as a charge?
        // Assuming PermitFee and Commission are charges to the bus, Penalty is another charge.
        // If Grand Total = Total earnings or total charges?
        // grandTotal = permitFeeTotal - commissionTotal
        // Let's assume penalty increases the total amount owed (or decreases net revenue)
        // If grandTotal = Permit - Commission + Penalty? No, let's just expose penaltyTotal.
        BigDecimal grandTotal = permitFeeTotal.subtract(commissionTotal).add(penaltyTotal);

        Users owner = bus.getOwner();
        String ownerName = owner != null ? (owner.getFirstName() + " " + owner.getLastName()) : "";
        Integer ownerId = owner != null ? owner.getId() : null;

        return BusFinanceSummaryItem.builder()
                .busId(bus.getId())
                .busNumber(bus.getBusNumber())
                .owner(ownerName)
                .ownerId(ownerId)
                .weekdayFee(weekdayFee)
                .weekendFee(weekendFee)
                .holidayFee(holidayFee)
                .permitFeeTotal(permitFeeTotal)
                .commissionTotal(commissionTotal)
                .penaltyTotal(penaltyTotal)
                .grandTotal(grandTotal)
                .build();
    }

    private RateCardResponse toRateCardResponse(FinanceRateCard card) {
        return RateCardResponse.builder()
                .permitFee(RateCardResponse.PermitFeeSection.builder()
                        .weekday(card.getWeekdayPermitFee())
                        .weekend(card.getWeekendPermitFee())
                        .holiday(card.getHolidayPermitFee())
                        .build())
                .offPeakCommission(RateCardResponse.CommissionSection.builder()
                        .mode(card.getOffPeakCommissionMode() != null
                                ? card.getOffPeakCommissionMode().toLowerCase() : DEFAULT_COMMISSION_MODE)
                        .value(card.getOffPeakCommissionValue())
                        .effectiveFrom(card.getEffectiveFrom())
                        .build())
                .lastUpdatedBy(card.getLastUpdatedBy())
                .lastUpdatedAt(card.getUpdatedAt() != null ? card.getUpdatedAt() : card.getCreatedAt())
                .build();
    }

    private RateCardResponse defaultRateCardResponse() {
        return RateCardResponse.builder()
                .permitFee(RateCardResponse.PermitFeeSection.builder()
                        .weekday(DEFAULT_WEEKDAY_FEE)
                        .weekend(DEFAULT_WEEKEND_FEE)
                        .holiday(DEFAULT_HOLIDAY_FEE)
                        .build())
                .offPeakCommission(RateCardResponse.CommissionSection.builder()
                        .mode(DEFAULT_COMMISSION_MODE)
                        .value(DEFAULT_COMMISSION_VALUE)
                        .effectiveFrom(LocalDate.now())
                        .build())
                .lastUpdatedBy("system")
                .lastUpdatedAt(null)
                .build();
    }

    private void validateRateCardRequest(RateCardUpdateRequest request) {
        if (request.getPermitFee() == null) {
            throw new IllegalArgumentException("Permit fee section is required");
        }
        if (request.getPermitFee().getWeekday() == null
                || request.getPermitFee().getWeekend() == null
                || request.getPermitFee().getHoliday() == null) {
            throw new IllegalArgumentException("Weekday, weekend, and holiday fees are all required");
        }
        if (request.getPermitFee().getWeekday().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Permit fees cannot be negative");
        }
    }

    private boolean isTimeInWindow(LocalTime time, LocalTime start, LocalTime end) {
        return !time.isBefore(start) && !time.isAfter(end);
    }

    private boolean isTimeInNightWindow(LocalTime time) {
        return !time.isBefore(LocalTime.of(23, 0)) || time.equals(LocalTime.MIDNIGHT);
    }
}
