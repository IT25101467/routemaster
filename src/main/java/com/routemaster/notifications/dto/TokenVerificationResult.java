package com.routemaster.notifications.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Inspection verification outcome for digital passenger boarding passes.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenVerificationResult {

    private boolean valid;
    private String message;
    private String bookingRef;
    private String passengerName;
    private String nic;
    private List<String> seatNos;
    private String tripRoute;
    private String tripDate;
    private String departureTime;
    private String bookingStatus;
    private String tokenHash;
}
