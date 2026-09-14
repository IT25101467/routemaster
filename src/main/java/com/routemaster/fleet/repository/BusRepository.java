package com.routemaster.fleet.repository;

import com.routemaster.fleet.entity.Bus;
import com.routemaster.fleet.entity.BusStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BusRepository extends JpaRepository<Bus, Long> {
    Optional<Bus> findByRegistrationNo(String registrationNo);
    List<Bus> findByStatus(BusStatus status);
}