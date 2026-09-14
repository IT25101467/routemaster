package com.routemaster.booking.entity;

import com.routemaster.common.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "passenger", indexes = {
        @Index(name = "idx_passenger_nic", columnList = "nic")
})
@Getter
@Setter
@NoArgsConstructor
public class Passenger extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "passenger_id")
    private Long passengerId;

    @NotBlank(message = "Passenger name is mandatory")
    @Column(nullable = false, length = 120)
    private String name;

    @NotBlank(message = "NIC / Passport is mandatory")
    @Column(nullable = false, length = 20)
    private String nic;

    @NotBlank(message = "Phone number is mandatory")
    @Column(nullable = false, length = 20)
    private String phone;

    @Column(length = 120)
    private String email;

    public Passenger(String name, String nic, String phone, String email) {
        this.name = name;
        this.nic = nic;
        this.phone = phone;
        this.email = email;
    }
}
