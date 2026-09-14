package com.routemaster.support.entity;

/**
 * Lifecycle states for Customer Complaints (Module F4 - Perera I.I.S. / IT25100685).
 * Governed by Rule BR-15 state machine rules.
 */
public enum ComplaintStatus {
    OPEN,
    IN_PROGRESS,
    RESOLVED,
    CLOSED,
    ESCALATED
}
