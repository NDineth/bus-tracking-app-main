package com.bustrackpro.service.tracking;

import com.bustrackpro.modal.tracking.AccelerationData;
import com.bustrackpro.util.DrivingMetrics;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DrivingMetricsTest {
    private AccelerationData reading(Double x, Double y) {
        return AccelerationData.builder().x(x).y(y).build();
    }

    @Test
    void matchesExistingAlertFormulaAndStrictThresholds() {
        var metrics = DrivingMetrics.from(List.of(reading(-3.0, 2.5), reading(-3.1, -2.6), reading(3.1, 0.0)));
        assertEquals(1, metrics.suddenBrakingCount());
        assertEquals(1, metrics.dangerousTurnCount());
        assertEquals(88, metrics.safetyScore());
    }

    @Test
    void absentAndPartialReadingsNeverBecomePerfectSafety() {
        assertNull(DrivingMetrics.from(null).safetyScore());
        assertNull(DrivingMetrics.from(List.of()).suddenBrakingCount());
        var partial = DrivingMetrics.from(List.of(reading(null, 0.0)));
        assertNull(partial.suddenBrakingCount());
        assertEquals(0, partial.dangerousTurnCount());
        assertNull(partial.safetyScore());
    }

    @Test
    void safeReadingsGetOneHundredAndLargePenaltiesClampToZero() {
        assertEquals(100, DrivingMetrics.from(List.of(reading(0.0, 0.0))).safetyScore());
        assertEquals(0, DrivingMetrics.from(java.util.Collections.nCopies(30, reading(-4.0, 3.0))).safetyScore());
    }
}
