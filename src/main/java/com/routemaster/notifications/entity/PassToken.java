package com.routemaster.notifications.entity;

import com.routemaster.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Cryptographic Digital Boarding Pass Token (Module F6 - Chathmal P.D.D. / IT25100115).
 * Stores SHA-256 digital signature of confirmed bookings for conductor/inspector verification.
 */
@Entity
@Table(name = "pass_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PassToken extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long tokenId;

    @Column(name = "token_hash", nullable = false, length = 64, unique = true)
    private String tokenHash;

    @Column(name = "booking_ref", nullable = false, length = 64, unique = true)
    private String bookingRef;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;
}
