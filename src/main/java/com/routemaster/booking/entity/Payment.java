package com.routemaster.booking.entity;

import com.routemaster.common.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payment", indexes = {
        @Index(name = "idx_payment_booking", columnList = "booking_id", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
public class Payment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long paymentId;

    @NotNull(message = "Booking ID is mandatory")
    @Column(name = "booking_id", nullable = false, unique = true)
    private Long bookingId;

    @NotNull(message = "Payment amount must be specified")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(length = 30)
    private String method;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private PaymentStatus status = PaymentStatus.COMPLETED;

    @NotNull
    @Column(name = "paid_at", nullable = false)
    private Instant paidAt;

    public Payment(Long bookingId, BigDecimal amount, String method, PaymentStatus status, Instant paidAt) {
        this.bookingId = bookingId;
        this.amount = amount;
        this.method = method;
        this.status = status;
        this.paidAt = paidAt;
    }
}
