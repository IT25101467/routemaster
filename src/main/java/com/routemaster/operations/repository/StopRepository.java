package com.routemaster.operations.repository;

import com.routemaster.operations.entity.Stop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StopRepository extends JpaRepository<Stop, Long> {
    List<Stop> findByCityIgnoreCase(String city);
}
