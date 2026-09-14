package com.routemaster.finance.repository;

import com.routemaster.finance.entity.Fare;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FareRepository extends JpaRepository<Fare, Long> {

    List<Fare> findByRouteId(Long routeId);

    List<Fare> findAllByOrderByEffectiveFromDesc();

    List<Fare> findByIsActiveTrueOrderByEffectiveFromDesc();

    Optional<Fare> findFirstByRouteIdOrderByEffectiveFromDesc(Long routeId);

    Optional<Fare> findFirstByRouteIdAndIsActiveTrueOrderByEffectiveFromDesc(Long routeId);
}
