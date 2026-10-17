package com.bustrackpro.modal.tracking;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/** One persisted, fleet-wide policy. Higher safety scores indicate safer driving. */
@Data
@Entity
public class AlertPolicy {
    @Id
    @JsonIgnore
    private Integer id = 1;

    @Min(1) @Max(98)
    private int criticalBelow = 40;
    @Min(2) @Max(99)
    private int highRiskBelow = 70;
    @Min(3) @Max(100)
    private int cautionBelow = 90;
    @Min(1) @Max(100)
    private int highSpeedEvents = 3;
    @Min(1) @Max(100)
    private int brakingEvents = 2;
    @Min(1) @Max(100)
    private int accelerationEvents = 2;
    @Min(1) @Max(100)
    private int turningEvents = 2;

    @AssertTrue(message = "Thresholds must satisfy 0 < critical < high risk < caution <= 100")
    @JsonIgnore
    public boolean isThresholdOrderValid() {
        return criticalBelow < highRiskBelow && highRiskBelow < cautionBelow;
    }

    public String severityFor(int score) {
        if (score < criticalBelow) return "CRITICAL";
        if (score < highRiskBelow) return "HIGH";
        if (score < cautionBelow) return "CAUTION";
        return "LOW";
    }

    public int eventsFor(DrivingBehaviorType behavior) {
        return switch (behavior) {
            case CRASH_DETECTED -> 1;
            case HIGH_SPEED -> highSpeedEvents;
            case SUDDEN_BRAKING -> brakingEvents;
            case SUDDEN_ACCELERATION -> accelerationEvents;
            case SUDDEN_TURNING -> turningEvents;
            default -> Integer.MAX_VALUE;
        };
    }
}
