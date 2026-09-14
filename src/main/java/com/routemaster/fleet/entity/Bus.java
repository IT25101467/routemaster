package com.routemaster.fleet.entity;

import com.routemaster.common.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "bus")
@Getter
@Setter
@NoArgsConstructor
public class Bus extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bus_id")
    private Long busId;

    @NotBlank(message = "Registration number is mandatory (BR-04)")
    @Column(name = "registration_no", nullable = false, unique = true, length = 20)
    private String registrationNo;

    @Column(length = 60)
    private String model;

    @Column(length = 60)
    private String manufacturer;

    @Column(name = "manufacture_year")
    private Integer manufactureYear;

    @Min(value = 10, message = "Seat capacity must be at least 10")
    @Max(value = 80, message = "Seat capacity cannot exceed 80")
    @Column(name = "seat_capacity", nullable = false)
    private Integer seatCapacity;

    @Column(name = "bus_type", length = 30)
    private String busType;

    @Column(length = 60)
    private String depot;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private BusStatus status = BusStatus.ACTIVE;

    @OneToMany(mappedBy = "bus", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Seat> seats = new ArrayList<>();

    @OneToMany(mappedBy = "bus", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ServiceRecord> serviceRecords = new ArrayList<>();
}