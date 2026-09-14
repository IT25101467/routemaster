package com.routemaster.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Summary projection of financial and operational performance per trip.
 * Implements Rule BR-17 (Completed payments only) and occupancy tracking.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TripFinancialSummaryDto {

    private Long tripId;
    private Long routeId;
    private String routeName;
    private String origin;
    private String destination;
    private LocalDate tripDate;
    private LocalTime departureTime;
    private int busCapacity;
    private int bookedSeatsCount;
    private double occupancyRate;
    private BigDecimal totalCompletedRevenue;
    private boolean isLossMaking;
}
