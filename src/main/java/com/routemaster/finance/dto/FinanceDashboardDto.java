package com.routemaster.finance.dto;

import com.routemaster.finance.entity.Fare;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinanceDashboardDto {

    private BigDecimal totalCompletedRevenue;
    private long totalCompletedBookings;
    private double averageOccupancy;
    private long lossMakingTripCount;
    private List<TripFinancialSummaryDto> tripSummaries;
    private List<Fare> fares;
}
