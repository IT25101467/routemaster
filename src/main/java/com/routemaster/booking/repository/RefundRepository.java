package com.routemaster.booking.repository;

import com.routemaster.booking.entity.Payment;
import com.routemaster.booking.entity.Refund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefundRepository extends JpaRepository<Refund, Long> {
    Optional<Refund> findByPayment(Payment payment);
    Optional<Refund> findByPaymentPaymentId(Long paymentId);
}
