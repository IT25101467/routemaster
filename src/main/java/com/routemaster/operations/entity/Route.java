package com.routemaster.operations.entity;

import com.routemaster.common.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "route")
@Getter
@Setter
@NoArgsConstructor
public class Route extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "route_id")
    private Long routeId;

    @NotBlank(message = "Route name is required")
    @Column(name = "route_name", nullable = false, length = 100)
    private String routeName;

    @NotBlank(message = "Origin is required")
    @Column(nullable = false, length = 80)
    private String origin;

    @NotBlank(message = "Destination is required")
    @Column(nullable = false, length = 80)
    private String destination;

    @NotNull(message = "Distance is required")
    @DecimalMin(value = "0.1", message = "Distance must be greater than 0")
    @Column(name = "distance_km", nullable = false, precision = 6, scale = 2)
    private BigDecimal distanceKm;

    @NotNull(message = "Estimated duration is required")
    @Min(value = 1, message = "Duration must be at least 1 minute")
    @Column(name = "est_duration_minutes", nullable = false)
    private Integer estDurationMinutes;

    @OneToMany(mappedBy = "route", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("stopSequence ASC")
    private java.util.List<RouteStop> stops = new java.util.ArrayList<>();

    @AssertTrue(message = "Origin and destination cannot be identical (BR-08)")
    public boolean isOriginDifferentFromDestination() {
        if (origin == null || destination == null) {
            return true;
        }
        return !origin.trim().equalsIgnoreCase(destination.trim());
    }

    public Route(String routeName, String origin, String destination, BigDecimal distanceKm, Integer estDurationMinutes) {
        this.routeName = routeName;
        this.origin = origin;
        this.destination = destination;
        this.distanceKm = distanceKm;
        this.estDurationMinutes = estDurationMinutes;
    }
}
