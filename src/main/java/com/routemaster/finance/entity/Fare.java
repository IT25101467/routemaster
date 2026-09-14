package com.routemaster.finance.entity;

import com.routemaster.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Route Fare Definition (Module F5 - Ranasinghe V.N. / IT25102675).
 * Governs dynamic route pricing, promotional discounts, and financial yield policies.
 */
@Entity
@Table(name = "fares")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Fare extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long fareId;

    @Column(name = "route_id", nullable = false)
    private Long routeId;

    @Column(name = "base_fare", nullable = false, precision = 10, scale = 2)
    private BigDecimal baseFare;

    @Column(name = "discount_pct", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal discountPct = BigDecimal.ZERO;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "surge_multiplier", precision = 4, scale = 2)
    @Builder.Default
    private BigDecimal surgeMultiplier = BigDecimal.valueOf(1.0);

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    public BigDecimal getSurgeMultiplier() {
        return surgeMultiplier != null ? surgeMultiplier : BigDecimal.valueOf(1.0);
    }

    public Boolean getIsActive() {
        return isActive != null ? isActive : true;
    }

    @jakarta.persistence.ManyToOne(fetch = jakarta.persistence.FetchType.LAZY)
    @jakarta.persistence.JoinColumn(name = "route_id", insertable = false, updatable = false)
    private com.routemaster.operations.entity.Route route;
}
