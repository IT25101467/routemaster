package com.routemaster.fleet.entity;

import com.routemaster.common.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "service_record")
@Getter
@Setter
@NoArgsConstructor
public class ServiceRecord extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "service_id")
    private Long serviceId;

    @NotNull(message = "Bus reference is mandatory")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bus_id", nullable = false)
    private Bus bus;

    @NotNull(message = "Service date is mandatory")
    @Column(name = "service_date", nullable = false)
    private LocalDate serviceDate;

    @NotBlank(message = "Service type is mandatory")
    @Column(name = "service_type", nullable = false, length = 60)
    private String serviceType;

    @PositiveOrZero(message = "Cost cannot be negative")
    @Column(precision = 10, scale = 2)
    private BigDecimal cost;

    @Column(length = 255)
    private String description;

    @Column(name = "next_service_due")
    private LocalDate nextServiceDue;

    public ServiceRecord(Bus bus, LocalDate serviceDate, String serviceType, BigDecimal cost, String description, LocalDate nextServiceDue) {
        this.bus = bus;
        this.serviceDate = serviceDate;
        this.serviceType = serviceType;
        this.cost = cost;
        this.description = description;
        this.nextServiceDue = nextServiceDue;
    }
}
