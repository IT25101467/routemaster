package com.routemaster.operations.repository;

import com.routemaster.operations.entity.DelayNotice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DelayNoticeRepository extends JpaRepository<DelayNotice, Long> {
    List<DelayNotice> findByTripIdOrderByPublishedAtDesc(Long tripId);
    Optional<DelayNotice> findFirstByTripIdOrderByPublishedAtDesc(Long tripId);
}
