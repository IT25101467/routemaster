package com.routemaster.operations.chain;

import com.routemaster.operations.entity.Trip;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

@Component
public class DriverDutyCheckHandler extends AssignmentValidationHandler {

    private static final ZoneId ZONE = ZoneId.of("Asia/Colombo");

    @Override
    protected void validate(AssignmentValidationContext context) {
        Long driverId = context.getDriverId();
        if (driverId == null || driverId <= 0) {
            throw new IllegalArgumentException("Valid Driver ID must be provided for trip assignment.");
        }

        if (context.getDriverTripsOnDay() == null || context.getDriverTripsOnDay().isEmpty()) {
            return;
        }

        for (Trip existingTrip : context.getDriverTripsOnDay()) {
            // Ignore current trip being updated
            if (context.getTrip() != null && context.getTrip().getTripId() != null 
                    && context.getTrip().getTripId().equals(existingTrip.getTripId())) {
                continue;
            }

            Instant existingStart = existingTrip.getTripDate()
                    .atTime(existingTrip.getDepartureTime())
                    .atZone(ZONE)
                    .toInstant();
            Instant existingEndWithBuffer = existingTrip.getTripDate()
                    .atTime(existingTrip.getArrivalTime())
                    .atZone(ZONE)
                    .toInstant()
                    .plus(30, ChronoUnit.MINUTES);

            // Overlap condition: requested_start < existing_end_with_buffer AND requested_end > existing_start
            if (context.getRequestedStart().isBefore(existingEndWithBuffer) 
                    && context.getRequestedEnd().isAfter(existingStart)) {
                throw new IllegalStateException("BR-10 Violation: Driver ID [" + driverId + 
                        "] is already rostered on conflicting Trip #" + existingTrip.getTripId() + 
                        " (" + existingTrip.getDepartureTime() + " - " + existingTrip.getArrivalTime() + 
                        " with 30m buffer).");
            }
        }
    }
}
