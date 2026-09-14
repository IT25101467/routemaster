package com.routemaster.booking.entity;

import com.routemaster.common.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "booking_seat", uniqueConstraints = {
        @UniqueConstraint(name = "uk_trip_bus_seat", columnNames = {"trip_id", "bus_id", "seat_no"})
})
@Getter
@Setter
@NoArgsConstructor
public class BookingSeat extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Booking reference is mandatory")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @NotNull(message = "Bus ID is mandatory")
    @Column(name = "bus_id", nullable = false)
    private Long busId;

    @NotBlank(message = "Seat number is mandatory")
    @Column(name = "seat_no", nullable = false, length = 10)
    private String seatNo;

    @NotNull(message = "Trip ID is mandatory")
    @Column(name = "trip_id", nullable = false)
    private Long tripId;

    public BookingSeat(Booking booking, Long busId, String seatNo, Long tripId) {
        this.booking = booking;
        this.busId = busId;
        this.seatNo = seatNo;
        this.tripId = tripId;
    }
}
