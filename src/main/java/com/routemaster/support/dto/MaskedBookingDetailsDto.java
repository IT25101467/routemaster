package com.routemaster.support.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Clean data transfer object implementing Rule BR-16 (Zero-Trust PII Masking).
 * Completely omits raw credit card PAN, CVV, and expiration dates from customer support views.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaskedBookingDetailsDto {

    private String bookingRef;
    private String passengerName;
    private String passengerPhone;
    private String passengerNic;
    private String passengerEmail;
    private Long tripId;
    private List<String> seatNos;
    private BigDecimal totalAmount;
    private String bookingStatus;

    // Masked Payment Information (BR-16)
    private String paymentMethod;
    private String paymentStatus;
    private String cardSecurityNotice; // e.g. "Card details omitted under Rule BR-16"
}
