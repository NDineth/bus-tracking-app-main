package com.bustrackpro.modal.tracking;

/**
 * Types of route compliance violations.
 */
public enum ViolationType {

    /** Bus has deviated from the assigned route corridor. */
    ROUTE_DEVIATION,

    /** Bus skipped one or more designated halts on the route. */
    HALT_SKIPPED,

    /** Abnormal or suspicious movement detected. */
    SUSPICIOUS_MOVEMENT,

    /** Speeding violation. */
    SPEEDING
}
