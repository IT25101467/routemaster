package com.routemaster.finance.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FareForm {

    @NotNull(message = "Route selection is required.")
    private Long routeId;

    @NotNull(message = "Base fare amount is required.")
    @DecimalMin(value = "10.00", message = "Base fare must be at least Rs. 10.00.")
    private BigDecimal baseFare;

    @DecimalMin(value = "0.00", message = "Discount cannot be negative.")
    @DecimalMax(value = "100.00", message = "Discount cannot exceed 100%.")
    private BigDecimal discountPct;

    @NotNull(message = "Effective from date is required.")
    private LocalDate effectiveFrom;

    private LocalDate effectiveTo;

    @DecimalMin(value = "0.50", message = "Surge multiplier must be at least 0.50.")
    @DecimalMax(value = "5.00", message = "Surge multiplier cannot exceed 5.00.")
    private BigDecimal surgeMultiplier;

    private Boolean isActive;
}
