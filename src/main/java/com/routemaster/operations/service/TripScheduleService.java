package com.routemaster.operations.service;

import com.routemaster.fleet.entity.Bus;
import com.routemaster.fleet.entity.BusStatus;
import com.routemaster.fleet.service.BusService;
import com.routemaster.operations.chain.AssignmentValidationContext;
import com.routemaster.operations.chain.TripAssignmentValidationChain;
import com.routemaster.operations.dto.DelayResponse;
import com.routemaster.operations.entity.DelayNotice;
import com.routemaster.operations.entity.Route;
import com.routemaster.operations.entity.RouteStop;
import com.routemaster.operations.entity.Trip;
import com.routemaster.operations.entity.TripStatus;
import com.routemaster.operations.repository.DelayNoticeRepository;
import com.routemaster.operations.repository.RouteRepository;
import com.routemaster.operations.repository.TripRepository;
import com.routemaster.common.event.TripDelayedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class TripScheduleService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Colombo");

    private final RouteRepository routeRepository;
    private final TripRepository tripRepository;
    private final DelayNoticeRepository delayNoticeRepository;
    private final TripAssignmentValidationChain assignmentValidationChain;
    private final BusService busService;
    private final ApplicationEventPublisher eventPublisher;

    @org.springframework.beans.factory.annotation.Autowired
    public TripScheduleService(RouteRepository routeRepository,
                               TripRepository tripRepository,
                               DelayNoticeRepository delayNoticeRepository,
                               TripAssignmentValidationChain assignmentValidationChain,
                               BusService busService,
                               ApplicationEventPublisher eventPublisher) {
        this.routeRepository = routeRepository;
        this.tripRepository = tripRepository;
        this.delayNoticeRepository = delayNoticeRepository;
        this.assignmentValidationChain = assignmentValidationChain;
        this.busService = busService;
        this.eventPublisher = eventPublisher;
    }

    public TripScheduleService(RouteRepository routeRepository,
                               TripRepository tripRepository,
                               DelayNoticeRepository delayNoticeRepository,
                               TripAssignmentValidationChain assignmentValidationChain,
                               BusService busService) {
        this(routeRepository, tripRepository, delayNoticeRepository, assignmentValidationChain, busService, null);
    }

    /**
     * Creates and saves a new transit route.
     * Enforces BR-08: origin != destination, and validates stop sequence ordering.
     */
    @Transactional
    public Route createRoute(Route route) {
        if (route.getOrigin() == null || route.getDestination() == null ||
                route.getOrigin().trim().equalsIgnoreCase(route.getDestination().trim())) {
            throw new IllegalArgumentException("BR-08 Violation: Route origin and destination cannot be identical.");
        }
        validateRouteStopSequence(route);
        return routeRepository.save(route);
    }

    /**
     * Validates that route stops have no duplicate sequences and are strictly ordered.
     */
    public void validateRouteStopSequence(Route route) {
        if (route.getStops() == null || route.getStops().isEmpty()) {
            return;
        }
        List<Integer> sequences = route.getStops().stream()
                .map(RouteStop::getStopSequence)
                .toList();
        Set<Integer> uniqueSequences = new HashSet<>(sequences);
        if (uniqueSequences.size() != sequences.size()) {
            throw new IllegalArgumentException("BR-08 / R05 Violation: Route stops contain duplicate sequence numbers.");
        }
        for (int i = 0; i < sequences.size(); i++) {
            if (sequences.get(i) == null || sequences.get(i) != i + 1) {
                throw new IllegalArgumentException("BR-08 / R05 Violation: Route stops must be strictly ordered starting from 1 sequentially.");
            }
        }
    }

    public List<Route> getAllRoutes() {
        return routeRepository.findAll();
    }

    public Route getRouteById(Long routeId) {
        return routeRepository.findById(routeId)
                .orElseThrow(() -> new IllegalArgumentException("Route #" + routeId + " not found."));
    }

    /**
     * Schedules a new trip for a route.
     * Starts in SCHEDULED status, unpublished until bus and driver are assigned.
     */
    @Transactional
    public Trip scheduleTrip(Long routeId, LocalDate date, LocalTime depTime, Integer durationMins) {
        Route route = getRouteById(routeId);
        int duration = (durationMins != null && durationMins > 0) ? durationMins : route.getEstDurationMinutes();
        LocalTime arrTime = depTime.plusMinutes(duration);

        Trip trip = new Trip(route, date, depTime, arrTime);
        return tripRepository.save(trip);
    }

    public List<Trip> getAllTrips() {
        return tripRepository.findAllByOrderByTripDateDescDepartureTimeAsc();
    }

    public Trip getTripById(Long tripId) {
        return tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Trip #" + tripId + " not found."));
    }

    /**
     * Assigns bus and driver using the Chain of Responsibility Pattern:
     * 1. BusStatusGateHandler (BR-05: Bus must be ACTIVE)
     * 2. BusOverlapCheckHandler (BR-06 & BR-07: Turnaround padding of 30 mins)
     * 3. DriverDutyCheckHandler (BR-10: Driver schedule conflict check)
     *
     * Upon successful validation, sets published = true (BR-09).
     */
    @Transactional
    public Trip assignBusAndDriver(Long tripId, Long busId, Long driverId) {
        Trip trip = getTripById(tripId);

        Instant requestedStart = trip.getTripDate()
                .atTime(trip.getDepartureTime())
                .atZone(ZONE)
                .toInstant();
        Instant requestedEnd = trip.getTripDate()
                .atTime(trip.getArrivalTime())
                .atZone(ZONE)
                .toInstant();

        List<Trip> busTripsOnDay = tripRepository.findActiveTripsForBusOnDate(busId, trip.getTripDate());
        List<Trip> driverTripsOnDay = tripRepository.findActiveTripsForDriverOnDate(driverId, trip.getTripDate());

        AssignmentValidationContext context = AssignmentValidationContext.builder()
                .trip(trip)
                .busId(busId)
                .driverId(driverId)
                .requestedStart(requestedStart)
                .requestedEnd(requestedEnd)
                .busTripsOnDay(busTripsOnDay)
                .driverTripsOnDay(driverTripsOnDay)
                .build();

        // Run validation chain
        assignmentValidationChain.execute(context);

        // Passed all gates - apply assignment
        trip.setBusId(busId);
        trip.setDriverId(driverId);

        // BR-09: Automatically mark published now that both bus and driver are assigned
        trip.setPublished(true);

        return tripRepository.save(trip);
    }

    /**
     * Assigns a replacement bus for disrupted trips (BR-12 & BR-13):
     * - Must verify latest delay notice is > 60 minutes.
     * - Must verify replacement bus capacity >= current bookings.
     * - Must verify replacement bus is ACTIVE and has no conflicting schedule.
     */
    @Transactional
    public Trip assignReplacementBus(Long tripId, Long newBusId) {
        Trip trip = getTripById(tripId);
        DelayNotice latestDelay = getLatestDelayNotice(tripId)
                .orElseThrow(() -> new IllegalStateException("BR-12 Violation: No delay notice recorded for Trip #" + tripId));

        if (latestDelay.getDelayMinutes() == null || latestDelay.getDelayMinutes() <= 60) {
            throw new IllegalStateException("BR-12 Violation: Replacement bus reallocation is strictly restricted to trips with delays exceeding 60 minutes (Current: " 
                    + latestDelay.getDelayMinutes() + " mins).");
        }

        Bus newBus = busService.getBusById(newBusId);
        if (newBus.getStatus() != BusStatus.ACTIVE) {
            throw new IllegalStateException("BR-05 Violation: Replacement bus [" + newBus.getRegistrationNo() 
                    + "] must be ACTIVE (Current status: " + newBus.getStatus() + ").");
        }

        int bookedSeats = getMockBookedSeats(tripId);
        if (newBus.getSeatCapacity() < bookedSeats) {
            throw new IllegalStateException("BR-13 Violation: Replacement bus capacity (" + newBus.getSeatCapacity() 
                    + ") cannot be less than current passenger bookings (" + bookedSeats + ").");
        }

        Instant requestedStart = trip.getTripDate()
                .atTime(trip.getDepartureTime())
                .atZone(ZONE)
                .toInstant();
        Instant requestedEnd = trip.getTripDate()
                .atTime(trip.getArrivalTime())
                .atZone(ZONE)
                .toInstant();

        List<Trip> busTripsOnDay = tripRepository.findActiveTripsForBusOnDate(newBusId, trip.getTripDate());
        List<BusService.TripInterval> existingIntervals = new ArrayList<>();
        for (Trip existingTrip : busTripsOnDay) {
            if (existingTrip.getTripId() != null && existingTrip.getTripId().equals(tripId)) {
                continue;
            }
            existingIntervals.add(new BusService.TripInterval(
                    existingTrip.getTripDate().atTime(existingTrip.getDepartureTime()).atZone(ZONE).toInstant(),
                    existingTrip.getTripDate().atTime(existingTrip.getArrivalTime()).atZone(ZONE).toInstant()
            ));
        }

        boolean available = busService.isBusAvailable(newBusId, requestedStart, requestedEnd, existingIntervals);
        if (!available) {
            throw new IllegalStateException("BR-06 / BR-07 Violation: Replacement bus has an overlapping trip or violates the 30-minute turnaround buffer.");
        }

        trip.setBusId(newBusId);
        return tripRepository.save(trip);
    }

    /**
     * Stub for current booked seats on a trip.
     * Integrates with Module F1 (Booking) once implemented.
     */
    public int getMockBookedSeats(Long tripId) {
        return 0;
    }

    /**
     * Publishes a delay notice for a trip.
     * Updates trip status to DELAYED and adjusts revised departure and arrival times.
     * Enforces BR-12: If delay > 60 mins, replacementRequired flag is set to true.
     */
    @Transactional
    public DelayResponse publishDelay(Long tripId, Integer delayMinutes, String reason) {
        if (delayMinutes == null || delayMinutes < 1) {
            throw new IllegalArgumentException("Delay minutes must be at least 1.");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Delay reason is mandatory.");
        }

        Trip trip = getTripById(tripId);

        LocalTime revisedDeparture = trip.getDepartureTime().plusMinutes(delayMinutes);
        LocalTime revisedArrival = trip.getArrivalTime().plusMinutes(delayMinutes);

        trip.setDepartureTime(revisedDeparture);
        trip.setArrivalTime(revisedArrival);
        trip.setStatus(TripStatus.DELAYED);

        DelayNotice notice = new DelayNotice(
                tripId,
                delayMinutes,
                reason.trim(),
                revisedDeparture,
                Instant.now()
        );

        tripRepository.save(trip);
        delayNoticeRepository.save(notice);

        // Observer Pattern: Broadcast disruption event across modules (observed by Module F6 - Notifications)
        if (eventPublisher != null) {
            eventPublisher.publishEvent(new TripDelayedEvent(tripId, delayMinutes, reason.trim()));
        }

        boolean replacementRequired = delayMinutes > 60; // BR-12
        String alertMsg = replacementRequired
                ? "CRITICAL ALERT (BR-12): Delay of " + delayMinutes + " minutes exceeds the 60-minute disruption threshold. Replacement bus reallocation required (BR-13)."
                : "Trip #" + tripId + " updated with a " + delayMinutes + "-minute delay notice.";

        return new DelayResponse(notice, trip, replacementRequired, alertMsg);
    }

    public Optional<DelayNotice> getLatestDelayNotice(Long tripId) {
        return delayNoticeRepository.findFirstByTripIdOrderByPublishedAtDesc(tripId);
    }

    public List<DelayNotice> getAllDelayNotices(Long tripId) {
        return delayNoticeRepository.findByTripIdOrderByPublishedAtDesc(tripId);
    }
}
