package com.routemaster.operations.entity;

import com.routemaster.common.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "trip")
@Getter
@Setter
@NoArgsConstructor
public class Trip extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "trip_id")
    private Long tripId;

    @NotNull(message = "Route is mandatory")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "route_id", nullable = false)
    private Route route;

    @Column(name = "bus_id")
    private Long busId;

    @Column(name = "driver_id")
    private Long driverId;

    @NotNull(message = "Trip date is mandatory")
    @Column(name = "trip_date", nullable = false)
    private LocalDate tripDate;

    @NotNull(message = "Departure time is mandatory")
    @Column(name = "departure_time", nullable = false)
    private LocalTime departureTime;

    @NotNull(message = "Arrival time is mandatory")
    @Column(name = "arrival_time", nullable = false)
    private LocalTime arrivalTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TripStatus status = TripStatus.SCHEDULED;

    @Column(nullable = false)
    private Boolean published = false;

    public Trip(Route route, LocalDate tripDate, LocalTime departureTime, LocalTime arrivalTime) {
        this.route = route;
        this.tripDate = tripDate;
        this.departureTime = departureTime;
        this.arrivalTime = arrivalTime;
        this.status = TripStatus.SCHEDULED;
        this.published = false;
    }
}
