package com.routemaster.fleet.service;

import com.routemaster.fleet.entity.Bus;
import com.routemaster.fleet.entity.BusStatus;
import com.routemaster.fleet.entity.Seat;
import com.routemaster.fleet.entity.ServiceRecord;
import com.routemaster.fleet.repository.BusRepository;
import com.routemaster.fleet.repository.ServiceRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class BusService {

    private final BusRepository busRepository;
    private final ServiceRecordRepository serviceRecordRepository;

    public BusService(BusRepository busRepository, ServiceRecordRepository serviceRecordRepository) {
        this.busRepository = busRepository;
        this.serviceRecordRepository = serviceRecordRepository;
    }

    public List<Bus> getAllBuses() {
        return busRepository.findAll();
    }

    public Bus getBusById(Long busId) {
        return busRepository.findById(busId)
                .orElseThrow(() -> new IllegalArgumentException("Bus with ID " + busId + " not found."));
    }

    @Transactional
    public Bus registerBus(Bus bus) {
        if (busRepository.findByRegistrationNo(bus.getRegistrationNo()).isPresent()) {
            throw new IllegalArgumentException("BR-04 Violation: Registration number " + bus.getRegistrationNo() + " already exists.");
        }

        // Auto-generate seats based on seatCapacity (Fork/Join parallel activity model in Lab 04)
        bus.getSeats().clear();
        for (int i = 1; i <= bus.getSeatCapacity(); i++) {
            String col = (i % 2 == 1) ? "A" : "B";
            int row = (i + 1) / 2;
            String seatNo = row + col;
            String position = (i % 4 == 1 || i % 4 == 0) ? "WINDOW" : "AISLE";
            bus.getSeats().add(new Seat(bus, seatNo, "STANDARD", position));
        }

        return busRepository.save(bus);
    }

    @Transactional
    public void updateBusStatus(Long busId, BusStatus newStatus) {
        Bus bus = getBusById(busId);
        if (bus.getStatus() == BusStatus.RETIRED && newStatus == BusStatus.ACTIVE) {
            throw new IllegalStateException("BR-05a Violation: A Retired bus cannot return to Active; it must be re-registered.");
        }
        bus.setStatus(newStatus);
        busRepository.save(bus);
    }

    /**
     * Rahman's Core Algorithm: Overlap Rule (BR-06) + 30-min Turnaround (BR-07)
     * Status Gating: Bus must be ACTIVE (BR-05).
     */
    public boolean checkBusAvailability(Long busId, Instant requestedStart, Instant requestedEnd, List<TripInterval> existingTrips) {
        Bus bus = getBusById(busId);
        if (bus.getStatus() != BusStatus.ACTIVE) {
            return false; // BR-05: Non-active buses cannot be assigned
        }

        if (existingTrips != null) {
            for (TripInterval trip : existingTrips) {
                Instant turnaroundEnd = trip.endTime().plus(30, ChronoUnit.MINUTES); // BR-07: 30-min buffer
                // Overlap check: requested_start < existing_end AND requested_end > existing_start
                if (requestedStart.isBefore(turnaroundEnd) && requestedEnd.isAfter(trip.startTime())) {
                    return false; // Clash detected (BR-06)
                }
            }
        }
        return true;
    }

    /**
     * Backward-compatible delegate for checkBusAvailability.
     */
    public boolean isBusAvailable(Long busId, Instant requestedStart, Instant requestedEnd, List<TripInterval> existingTrips) {
        return checkBusAvailability(busId, requestedStart, requestedEnd, existingTrips);
    }

    @Transactional
    public ServiceRecord addServiceRecord(Long busId, ServiceRecord record) {
        Bus bus = getBusById(busId);
        record.setBus(bus);
        return serviceRecordRepository.save(record);
    }

    public List<ServiceRecord> getServiceRecords(Long busId) {
        return serviceRecordRepository.findByBusBusIdOrderByServiceDateDesc(busId);
    }

    public record TripInterval(Instant startTime, Instant endTime) {}
}