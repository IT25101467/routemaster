package com.routemaster.operations.chain;

import com.routemaster.fleet.entity.Bus;
import com.routemaster.fleet.entity.BusStatus;
import com.routemaster.fleet.service.BusService;
import org.springframework.stereotype.Component;

@Component
public class BusStatusGateHandler extends AssignmentValidationHandler {

    private final BusService busService;

    public BusStatusGateHandler(BusService busService) {
        this.busService = busService;
    }

    @Override
    protected void validate(AssignmentValidationContext context) {
        if (context.getBusId() == null) {
            throw new IllegalArgumentException("Bus ID must be specified for trip assignment.");
        }

        Bus bus = busService.getBusById(context.getBusId());
        if (bus.getStatus() != BusStatus.ACTIVE) {
            throw new IllegalStateException("BR-05 Violation: Bus [" + bus.getRegistrationNo() + 
                    "] cannot be assigned because its status is " + bus.getStatus() + " (Only ACTIVE buses allowed).");
        }
    }
}
