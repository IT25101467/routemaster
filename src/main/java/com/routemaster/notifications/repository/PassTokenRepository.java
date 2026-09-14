package com.routemaster.notifications.repository;

import com.routemaster.notifications.entity.PassToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PassTokenRepository extends JpaRepository<PassToken, Long> {

    Optional<PassToken> findByBookingRef(String bookingRef);

    Optional<PassToken> findByTokenHash(String tokenHash);
}
