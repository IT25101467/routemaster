package com.routemaster.operations.chain;

import com.routemaster.operations.entity.Trip;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
@Builder
public class AssignmentValidationContext {
    private final Trip trip;
    private final Long busId;
    private final Long driverId;
    private final Instant requestedStart;
    private final Instant requestedEnd;
    private final List<Trip> busTripsOnDay;
    private final List<Trip> driverTripsOnDay;
}
