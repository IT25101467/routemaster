package com.routemaster.operations.dto;

import com.routemaster.operations.entity.DelayNotice;
import com.routemaster.operations.entity.Trip;

public record DelayResponse(
        DelayNotice delayNotice,
        Trip trip,
        boolean replacementRequired,
        String message
) {}
