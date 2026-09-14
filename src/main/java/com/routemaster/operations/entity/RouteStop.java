package com.routemaster.operations.entity;

import com.routemaster.common.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "route_stop")
@Getter
@Setter
@NoArgsConstructor
public class RouteStop extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "route_stop_id")
    private Long routeStopId;

    @NotNull(message = "Route reference is mandatory")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id", nullable = false)
    private Route route;

    @NotNull(message = "Stop reference is mandatory")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "stop_id", nullable = false)
    private Stop stop;

    @NotNull(message = "Stop sequence is mandatory")
    @Min(value = 1, message = "Sequence must start at 1")
    @Column(name = "stop_sequence", nullable = false)
    private Integer stopSequence;

    @NotNull(message = "Arrival offset minutes must be specified")
    @Min(value = 0, message = "Offset minutes cannot be negative")
    @Column(name = "arrival_offset_minutes", nullable = false)
    private Integer arrivalOffsetMinutes;

    public RouteStop(Route route, Stop stop, Integer stopSequence, Integer arrivalOffsetMinutes) {
        this.route = route;
        this.stop = stop;
        this.stopSequence = stopSequence;
        this.arrivalOffsetMinutes = arrivalOffsetMinutes;
    }
}
