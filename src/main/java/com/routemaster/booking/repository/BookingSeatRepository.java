package com.routemaster.booking.repository;

import com.routemaster.booking.entity.Booking;
import com.routemaster.booking.entity.BookingSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingSeatRepository extends JpaRepository<BookingSeat, Long> {
    List<BookingSeat> findByTripId(Long tripId);
    List<BookingSeat> findByTripIdAndBusId(Long tripId, Long busId);
    List<BookingSeat> findByBooking(Booking booking);
    boolean existsByTripIdAndBusIdAndSeatNo(Long tripId, Long busId, String seatNo);
    long countByTripId(Long tripId);
}
