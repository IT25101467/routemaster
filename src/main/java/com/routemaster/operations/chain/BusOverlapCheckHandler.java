package com.routemaster.operations.chain;

import com.routemaster.fleet.service.BusService;
import com.routemaster.operations.entity.Trip;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Component
public class BusOverlapCheckHandler extends AssignmentValidationHandler {

    private static final ZoneId ZONE = ZoneId.of("Asia/Colombo");
    private final BusService busService;

    public BusOverlapCheckHandler(BusService busService) {
        this.busService = busService;
    }

    @Override
    protected void validate(AssignmentValidationContext context) {
        List<BusService.TripInterval> existingIntervals = new ArrayList<>();

        if (context.getBusTripsOnDay() != null) {
            for (Trip existingTrip : context.getBusTripsOnDay()) {
                // Ignore the trip currently being updated
                if (context.getTrip() != null && context.getTrip().getTripId() != null 
                        && context.getTrip().getTripId().equals(existingTrip.getTripId())) {
                    continue;
                }

                Instant start = existingTrip.getTripDate()
                        .atTime(existingTrip.getDepartureTime())
                        .atZone(ZONE)
                        .toInstant();
                Instant end = existingTrip.getTripDate()
                        .atTime(existingTrip.getArrivalTime())
                        .atZone(ZONE)
                        .toInstant();

                existingIntervals.add(new BusService.TripInterval(start, end));
            }
        }

        boolean available = busService.isBusAvailable(
                context.getBusId(),
                context.getRequestedStart(),
                context.getRequestedEnd(),
                existingIntervals
        );

        if (!available) {
            throw new IllegalStateException("BR-06 / BR-07 Violation: Bus schedule conflict detected. " +
                    "A bus cannot overlap with existing trips and requires a minimum 30-minute turnaround buffer.");
        }
    }
}
