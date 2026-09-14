package com.routemaster.operations.repository;

import com.routemaster.operations.entity.Trip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TripRepository extends JpaRepository<Trip, Long> {

    List<Trip> findByBusIdAndTripDate(Long busId, LocalDate tripDate);

    List<Trip> findByDriverIdAndTripDate(Long driverId, LocalDate tripDate);

    List<Trip> findAllByOrderByTripDateDescDepartureTimeAsc();

    List<Trip> findByPublishedTrueOrderByTripDateAscDepartureTimeAsc();

    @Query("SELECT t FROM Trip t WHERE t.busId = :busId AND t.tripDate = :tripDate AND t.status != 'CANCELLED'")
    List<Trip> findActiveTripsForBusOnDate(@Param("busId") Long busId, @Param("tripDate") LocalDate tripDate);

    @Query("SELECT t FROM Trip t WHERE t.driverId = :driverId AND t.tripDate = :tripDate AND t.status != 'CANCELLED'")
    List<Trip> findActiveTripsForDriverOnDate(@Param("driverId") Long driverId, @Param("tripDate") LocalDate tripDate);
}
