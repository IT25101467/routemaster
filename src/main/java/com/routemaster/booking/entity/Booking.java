package com.routemaster.booking.entity;

import com.routemaster.common.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "booking", indexes = {
        @Index(name = "idx_booking_ref", columnList = "booking_ref", unique = true),
        @Index(name = "idx_booking_trip", columnList = "trip_id")
})
@Getter
@Setter
@NoArgsConstructor
public class Booking extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "booking_id")
    private Long bookingId;

    @NotBlank(message = "Booking reference is mandatory (BR-03)")
    @Column(name = "booking_ref", nullable = false, unique = true, length = 64)
    private String bookingRef;

    @NotNull(message = "Passenger association is mandatory (R15)")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "passenger_id", nullable = false)
    private Passenger passenger;

    @NotNull(message = "Trip reference is mandatory")
    @Column(name = "trip_id", nullable = false)
    private Long tripId;

    @NotNull(message = "Total amount must be specified")
    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private BookingStatus status = BookingStatus.CONFIRMED;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BookingSeat> seats = new ArrayList<>();

    public Booking(String bookingRef, Passenger passenger, Long tripId, BigDecimal totalAmount, BookingStatus status) {
        this.bookingRef = bookingRef;
        this.passenger = passenger;
        this.tripId = tripId;
        this.totalAmount = totalAmount;
        this.status = status;
    }
}
