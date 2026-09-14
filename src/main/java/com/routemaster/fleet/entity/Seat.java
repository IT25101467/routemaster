package com.routemaster.fleet.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "seat")
@Getter
@Setter
@NoArgsConstructor
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bus_id", nullable = false)
    private Bus bus;

    @Column(name = "seat_no", nullable = false, length = 5)
    private String seatNo;

    @Column(name = "seat_type", length = 20)
    private String seatType;

    @Column(length = 20)
    private String position;

    public Seat(Bus bus, String seatNo, String seatType, String position) {
        this.bus = bus;
        this.seatNo = seatNo;
        this.seatType = seatType;
        this.position = position;
    }
}