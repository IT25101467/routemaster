package com.routemaster.finance.service;

import com.routemaster.booking.entity.Booking;
import com.routemaster.booking.entity.BookingStatus;
import com.routemaster.booking.entity.Payment;
import com.routemaster.booking.entity.PaymentStatus;
import com.routemaster.booking.repository.BookingRepository;
import com.routemaster.booking.repository.PaymentRepository;
import com.routemaster.fleet.entity.Bus;
import com.routemaster.fleet.repository.BusRepository;
import com.routemaster.finance.dto.FareForm;
import com.routemaster.finance.dto.FinanceDashboardDto;
import com.routemaster.finance.dto.TripFinancialSummaryDto;
import com.routemaster.finance.entity.Fare;
import com.routemaster.finance.repository.FareRepository;
import com.routemaster.operations.entity.Trip;
import com.routemaster.operations.repository.TripRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Financial analytics and fare management engine (Module F5 - Ranasinghe V.N. / IT25102675).
 * Enforces Rule BR-17: Revenue calculations strictly filter for PaymentStatus.COMPLETED.
 */
@Service
public class FinanceService {

    private final FareRepository fareRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final TripRepository tripRepository;
    private final BusRepository busRepository;

    public FinanceService(FareRepository fareRepository,
                          BookingRepository bookingRepository,
                          PaymentRepository paymentRepository,
                          TripRepository tripRepository,
                          BusRepository busRepository) {
        this.fareRepository = fareRepository;
        this.bookingRepository = bookingRepository;
        this.paymentRepository = paymentRepository;
        this.tripRepository = tripRepository;
        this.busRepository = busRepository;
    }

    /**
     * Compiles comprehensive financial and occupancy analytics for the executive dashboard.
     * Enforces Rule BR-17 (Completed payments only) and occupancy calculations.
     */
    @Transactional(readOnly = true)
    public FinanceDashboardDto getDashboardData() {
        List<Trip> allTrips = tripRepository.findAll();
        Map<Long, Bus> busMap = busRepository.findAll().stream()
                .collect(Collectors.toMap(Bus::getBusId, Function.identity(), (a, b) -> a));

        List<TripFinancialSummaryDto> tripSummaries = new ArrayList<>();
        BigDecimal grandTotalRevenue = BigDecimal.ZERO;
        long totalCompletedBookings = 0;
        long lossMakingCount = 0;
        double sumOccupancy = 0.0;

        for (Trip trip : allTrips) {
            List<Booking> tripBookings = bookingRepository.findByTripId(trip.getTripId());

            // BR-17: Filter and aggregate ONLY completed payments
            BigDecimal tripRevenue = BigDecimal.ZERO;
            int bookedSeats = 0;

            for (Booking b : tripBookings) {
                if (b.getStatus() == BookingStatus.CONFIRMED) {
                    Optional<Payment> paymentOpt = paymentRepository.findByBookingId(b.getBookingId());
                    if (paymentOpt.isPresent() && paymentOpt.get().getStatus() == PaymentStatus.COMPLETED) {
                        tripRevenue = tripRevenue.add(b.getTotalAmount() != null ? b.getTotalAmount() : BigDecimal.ZERO);
                        totalCompletedBookings++;
                    }
                    if (b.getSeats() != null) {
                        bookedSeats += b.getSeats().size();
                    }
                }
            }

            int capacity = 40; // standard fallback
            if (trip.getBusId() != null && busMap.containsKey(trip.getBusId())) {
                capacity = busMap.get(trip.getBusId()).getSeatCapacity();
            }

            double occupancy = capacity > 0
                    ? BigDecimal.valueOf((double) bookedSeats / capacity * 100.0)
                        .setScale(1, RoundingMode.HALF_UP).doubleValue()
                    : 0.0;

            boolean lossMaking = occupancy < 40.0;
            if (lossMaking) {
                lossMakingCount++;
            }

            sumOccupancy += occupancy;
            grandTotalRevenue = grandTotalRevenue.add(tripRevenue);

            tripSummaries.add(TripFinancialSummaryDto.builder()
                    .tripId(trip.getTripId())
                    .routeId(trip.getRoute() != null ? trip.getRoute().getRouteId() : null)
                    .routeName(trip.getRoute() != null ? trip.getRoute().getRouteName() : "Unassigned Route")
                    .origin(trip.getRoute() != null ? trip.getRoute().getOrigin() : "-")
                    .destination(trip.getRoute() != null ? trip.getRoute().getDestination() : "-")
                    .tripDate(trip.getTripDate())
                    .departureTime(trip.getDepartureTime())
                    .busCapacity(capacity)
                    .bookedSeatsCount(bookedSeats)
                    .occupancyRate(occupancy)
                    .totalCompletedRevenue(tripRevenue)
                    .isLossMaking(lossMaking)
                    .build());
        }

        double avgOccupancy = allTrips.isEmpty() ? 0.0 :
                BigDecimal.valueOf(sumOccupancy / allTrips.size()).setScale(1, RoundingMode.HALF_UP).doubleValue();

        List<Fare> fares = fareRepository.findAllByOrderByEffectiveFromDesc();

        return FinanceDashboardDto.builder()
                .totalCompletedRevenue(grandTotalRevenue)
                .totalCompletedBookings(totalCompletedBookings)
                .averageOccupancy(avgOccupancy)
                .lossMakingTripCount(lossMakingCount)
                .tripSummaries(tripSummaries)
                .fares(fares)
                .build();
    }

    /**
     * Registers a new route fare definition.
     */
    @Transactional
    public Fare createFare(FareForm form) {
        Fare fare = Fare.builder()
                .routeId(form.getRouteId())
                .baseFare(form.getBaseFare())
                .discountPct(form.getDiscountPct() != null ? form.getDiscountPct() : BigDecimal.ZERO)
                .effectiveFrom(form.getEffectiveFrom())
                .effectiveTo(form.getEffectiveTo())
                .surgeMultiplier(form.getSurgeMultiplier() != null ? form.getSurgeMultiplier() : BigDecimal.valueOf(1.0))
                .isActive(form.getIsActive() != null ? form.getIsActive() : true)
                .build();

        return fareRepository.save(fare);
    }

    /**
     * Updates an existing route fare definition.
     */
    @Transactional
    public Fare updateFare(Long fareId, FareForm form) {
        Fare fare = fareRepository.findById(fareId)
                .orElseThrow(() -> new IllegalArgumentException("Fare definition not found with ID: " + fareId));

        fare.setRouteId(form.getRouteId());
        fare.setBaseFare(form.getBaseFare());
        fare.setDiscountPct(form.getDiscountPct() != null ? form.getDiscountPct() : BigDecimal.ZERO);
        fare.setEffectiveFrom(form.getEffectiveFrom());
        fare.setEffectiveTo(form.getEffectiveTo());
        if (form.getSurgeMultiplier() != null) {
            fare.setSurgeMultiplier(form.getSurgeMultiplier());
        }
        if (form.getIsActive() != null) {
            fare.setIsActive(form.getIsActive());
        }

        return fareRepository.save(fare);
    }

    /**
     * Deletes a route fare policy.
     */
    @Transactional
    public void deleteFare(Long fareId) {
        if (!fareRepository.existsById(fareId)) {
            throw new IllegalArgumentException("Fare definition not found with ID: " + fareId);
        }
        fareRepository.deleteById(fareId);
    }

    /**
     * Retrieves a single fare definition by ID.
     */
    @Transactional(readOnly = true)
    public Fare getFareById(Long fareId) {
        return fareRepository.findById(fareId)
                .orElseThrow(() -> new IllegalArgumentException("Fare definition not found with ID: " + fareId));
    }

    /**
     * Retrieves all fare definitions ordered by effective date.
     */
    @Transactional(readOnly = true)
    public List<Fare> getAllFares() {
        return fareRepository.findAllByOrderByEffectiveFromDesc();
    }

    /**
     * Calculates dynamic trip fare based on active route base price, surge pricing multipliers,
     * and promotional discounts for a given number of reserved seats.
     *
     * INTEGRATION HOOK FOR MODULE F1 (Booking):
     * F1 BookingService should invoke this method to calculate the dynamic total amount
     * when confirming reservations instead of relying on static flat-rate pricing.
     *
     * Formula: Total = ((BaseFare * (1 - Discount%)) * SurgeMultiplier) * SeatCount
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateDynamicFare(Long routeId, int seatCount) {
        if (seatCount <= 0) {
            return BigDecimal.ZERO;
        }

        Fare fare = (routeId != null)
                ? fareRepository.findFirstByRouteIdAndIsActiveTrueOrderByEffectiveFromDesc(routeId).orElse(null)
                : null;

        BigDecimal baseFare = (fare != null && fare.getBaseFare() != null)
                ? fare.getBaseFare()
                : BigDecimal.valueOf(1500.00); // Standard RouteMaster default base fare

        // Apply promotional discount if configured
        if (fare != null && fare.getDiscountPct() != null && fare.getDiscountPct().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal discountFactor = BigDecimal.ONE.subtract(
                    fare.getDiscountPct().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
            baseFare = baseFare.multiply(discountFactor).setScale(2, RoundingMode.HALF_UP);
        }

        // Apply real-time surge multiplier (default 1.0)
        BigDecimal multiplier = (fare != null && fare.getSurgeMultiplier() != null)
                ? fare.getSurgeMultiplier()
                : BigDecimal.valueOf(1.0);

        BigDecimal pricePerSeat = baseFare.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
        return pricePerSeat.multiply(BigDecimal.valueOf(seatCount)).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Generates a downloadable CSV audit export of financial performance across all operational trips.
     */
    @Transactional(readOnly = true)
    public byte[] generateFinancialCsvReport() {
        FinanceDashboardDto dashboard = getDashboardData();
        StringBuilder sb = new StringBuilder();

        // CSV Header
        sb.append("Trip ID,Route Name,Origin,Destination,Travel Date,Departure Time,Bus Capacity,Booked Seats,Occupancy Rate (%),Completed Revenue (LKR),Loss-Making Flag\n");

        for (TripFinancialSummaryDto t : dashboard.getTripSummaries()) {
            sb.append(t.getTripId()).append(",")
                    .append("\"").append(t.getRouteName()).append("\",")
                    .append("\"").append(t.getOrigin()).append("\",")
                    .append("\"").append(t.getDestination()).append("\",")
                    .append(t.getTripDate()).append(",")
                    .append(t.getDepartureTime()).append(",")
                    .append(t.getBusCapacity()).append(",")
                    .append(t.getBookedSeatsCount()).append(",")
                    .append(t.getOccupancyRate()).append(",")
                    .append(t.getTotalCompletedRevenue()).append(",")
                    .append(t.isLossMaking() ? "YES (<40%)" : "NO")
                    .append("\n");
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }
}
