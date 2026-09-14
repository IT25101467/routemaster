package com.routemaster.common.event;

/**
 * Domain event published by Module F3 (Operations) and observed by Module F6 (Notifications).
 * Implements the Observer Pattern for real-time passenger disruption alert broadcasting (Rule BR-11).
 */
public record TripDelayedEvent(
        Long tripId,
        Integer delayMinutes,
        String reason
) {
}
