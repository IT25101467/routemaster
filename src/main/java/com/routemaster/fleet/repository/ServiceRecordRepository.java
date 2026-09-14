package com.routemaster.fleet.repository;

import com.routemaster.fleet.entity.Bus;
import com.routemaster.fleet.entity.ServiceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceRecordRepository extends JpaRepository<ServiceRecord, Long> {
    List<ServiceRecord> findByBusOrderByServiceDateDesc(Bus bus);
    List<ServiceRecord> findByBusBusIdOrderByServiceDateDesc(Long busId);
}
