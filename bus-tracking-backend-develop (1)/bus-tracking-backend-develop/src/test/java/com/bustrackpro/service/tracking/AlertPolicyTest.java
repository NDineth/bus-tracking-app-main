package com.bustrackpro.service.tracking;

import com.bustrackpro.modal.tracking.AlertPolicy;
import com.bustrackpro.modal.tracking.DrivingBehaviorType;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AlertPolicyTest {
    @Test
    void scoreBoundaryEqualityEntersTheSaferBand() {
        AlertPolicy policy = new AlertPolicy();
        assertEquals("CRITICAL", policy.severityFor(0));
        assertEquals("CRITICAL", policy.severityFor(39));
        assertEquals("HIGH", policy.severityFor(40));
        assertEquals("HIGH", policy.severityFor(69));
        assertEquals("CAUTION", policy.severityFor(70));
        assertEquals("CAUTION", policy.severityFor(89));
        assertEquals("LOW", policy.severityFor(90));
        assertEquals("LOW", policy.severityFor(100));
    }

    @Test
    void invalidPolicyCannotPassBeanValidation() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            AlertPolicy policy = new AlertPolicy();
            assertTrue(validator.validate(policy).isEmpty());
            policy.setCriticalBelow(70);
            assertFalse(validator.validate(policy).isEmpty());
            policy.setCriticalBelow(40);
            policy.setBrakingEvents(0);
            assertFalse(validator.validate(policy).isEmpty());
            policy.setBrakingEvents(101);
            assertFalse(validator.validate(policy).isEmpty());
        }
    }

    @Test
    void crashAlwaysTriggersOnTheFirstEvent() {
        AlertPolicy policy = new AlertPolicy();
        policy.setHighSpeedEvents(100);
        assertEquals(1, policy.eventsFor(DrivingBehaviorType.CRASH_DETECTED));
        assertEquals(100, policy.eventsFor(DrivingBehaviorType.HIGH_SPEED));
    }
}
