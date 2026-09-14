package com.routemaster.booking.entity;

import com.routemaster.common.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "refund")
@Getter
@Setter
@NoArgsConstructor
public class Refund extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "refund_id")
    private Long refundId;

    @NotNull(message = "Payment reference is mandatory for refund")
    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "payment_id", nullable = false, unique = true)
    private Payment payment;

    @NotNull(message = "Refund amount must be specified")
    @Column(name = "refund_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal refundAmount;

    @Column(length = 255)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private RefundStatus status = RefundStatus.PENDING;

    public Refund(Payment payment, BigDecimal refundAmount, String reason, RefundStatus status) {
        this.payment = payment;
        this.refundAmount = refundAmount;
        this.reason = reason;
        this.status = status;
    }
}
