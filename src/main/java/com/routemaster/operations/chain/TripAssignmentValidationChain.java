package com.routemaster.operations.chain;

import org.springframework.stereotype.Component;

@Component
public class TripAssignmentValidationChain {

    private final BusStatusGateHandler busStatusGateHandler;
    private final BusOverlapCheckHandler busOverlapCheckHandler;
    private final DriverDutyCheckHandler driverDutyCheckHandler;

    public TripAssignmentValidationChain(BusStatusGateHandler busStatusGateHandler,
                                         BusOverlapCheckHandler busOverlapCheckHandler,
                                         DriverDutyCheckHandler driverDutyCheckHandler) {
        this.busStatusGateHandler = busStatusGateHandler;
        this.busOverlapCheckHandler = busOverlapCheckHandler;
        this.driverDutyCheckHandler = driverDutyCheckHandler;
    }

    public void execute(AssignmentValidationContext context) {
        // Link the chain: StatusGate -> OverlapCheck -> DriverDutyCheck
        busStatusGateHandler
                .setNext(busOverlapCheckHandler)
                .setNext(driverDutyCheckHandler);

        busStatusGateHandler.handle(context);
    }
}
