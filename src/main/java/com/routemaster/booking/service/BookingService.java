package com.routemaster.booking.service;

import com.routemaster.booking.dto.SeatMapItemDto;
import com.routemaster.booking.entity.*;
import com.routemaster.booking.lock.SeatLockService;
import com.routemaster.booking.repository.*;
import com.routemaster.fleet.entity.Bus;
import com.routemaster.fleet.entity.Seat;
import com.routemaster.fleet.service.BusService;
import com.routemaster.operations.entity.Trip;
import com.routemaster.operations.entity.TripStatus;
import com.routemaster.operations.service.TripScheduleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BookingService {

    private static final BigDecimal DEFAULT_FARE_PER_SEAT = new BigDecimal("1500.00");
    private static final int MAX_SEATS_PER_BOOKING = 4;

    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final PassengerRepository passengerRepository;
    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final SeatLockService seatLockService;
    private final TripScheduleService tripScheduleService;
    private final BusService busService;

    public BookingService(BookingRepository bookingRepository,
                          BookingSeatRepository bookingSeatRepository,
                          PassengerRepository passengerRepository,
                          PaymentRepository paymentRepository,
                          RefundRepository refundRepository,
                          SeatLockService seatLockService,
                          TripScheduleService tripScheduleService,
                          BusService busService) {
        this.bookingRepository = bookingRepository;
        this.bookingSeatRepository = bookingSeatRepository;
        this.passengerRepository = passengerRepository;
        this.paymentRepository = paymentRepository;
        this.refundRepository = refundRepository;
        this.seatLockService = seatLockService;
        this.tripScheduleService = tripScheduleService;
        this.busService = busService;
    }

    /**
     * Retrieves seat map layout for a trip.
     * Evaluates state as BOOKED (in DB), LOCKED (in Redis cache), or AVAILABLE.
     */
    public List<SeatMapItemDto> getSeatMap(Long tripId) {
        Trip trip = tripScheduleService.getTripById(tripId);
        if (trip.getBusId() == null) {
            throw new IllegalStateException("Cannot render seat map: Vehicle has not yet been assigned to Trip #" + tripId);
        }

        Bus bus = busService.getBusById(trip.getBusId());
        Set<String> bookedSeats = bookingSeatRepository.findByTripIdAndBusId(tripId, trip.getBusId()).stream()
                .map(BookingSeat::getSeatNo)
                .collect(Collectors.toSet());

        List<SeatMapItemDto> seatMap = new ArrayList<>();
        for (Seat seat : bus.getSeats()) {
            String seatNo = seat.getSeatNo();
            SeatState state;
            long remainingTtl = 0;

            if (bookedSeats.contains(seatNo)) {
                state = SeatState.BOOKED;
            } else if (seatLockService.isSeatLocked(tripId, seatNo)) {
                state = SeatState.LOCKED;
                remainingTtl = seatLockService.getRemainingTtl(tripId, seatNo);
            } else {
                state = SeatState.AVAILABLE;
            }

            seatMap.add(new SeatMapItemDto(
                    seatNo,
                    seat.getSeatType(),
                    seat.getPosition(),
                    state,
                    remainingTtl
            ));
        }

        return seatMap;
    }

    /**
     * Executes atomic booking confirmation (BR-02, BR-03):
     * 1. Resolves or creates Passenger record (R15).
     * 2. Checks database double booking fail-safe.
     * 3. Creates Booking and BookingSeat entries.
     * 4. Issues Payment with status COMPLETED (BR-02 gating).
     * 5. Releases Redis distributed locks.
     */
    @Transactional
    public Booking confirmBooking(Long tripId, List<String> seatNos,
                                  String passengerName, String phone, String nic, String email,
                                  String holdToken) {
        if (seatNos == null || seatNos.isEmpty()) {
            throw new IllegalArgumentException("At least one seat must be selected.");
        }
        if (seatNos.size() > MAX_SEATS_PER_BOOKING) {
            throw new IllegalArgumentException("Exceeded maximum limit of " + MAX_SEATS_PER_BOOKING + " seats per reservation transaction.");
        }

        Trip trip = tripScheduleService.getTripById(tripId);
        if (!Boolean.TRUE.equals(trip.getPublished())) {
            throw new IllegalStateException("Cannot reserve seats: Trip #" + tripId + " is not yet published.");
        }

        // Database fail-safe check (BR-01 / BR-02)
        for (String seatNo : seatNos) {
            if (bookingSeatRepository.existsByTripIdAndBusIdAndSeatNo(tripId, trip.getBusId(), seatNo)) {
                throw new IllegalStateException("Transactional Fail-safe Alert: Seat [" + seatNo + "] is already booked in database.");
            }
        }

        // 1. Resolve or create Passenger (R15)
        Passenger passenger = passengerRepository.findByNic(nic.trim())
                .map(existing -> {
                    existing.setName(passengerName.trim());
                    existing.setPhone(phone.trim());
                    if (email != null && !email.isBlank()) {
                        existing.setEmail(email.trim());
                    }
                    return passengerRepository.save(existing);
                })
                .orElseGet(() -> passengerRepository.save(new Passenger(
                        passengerName.trim(),
                        nic.trim(),
                        phone.trim(),
                        (email != null && !email.isBlank()) ? email.trim() : null
                )));

        // 2. Generate unique non-reusable booking reference (BR-03)
        String bookingRef = "RM-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();

        BigDecimal totalAmount = DEFAULT_FARE_PER_SEAT.multiply(BigDecimal.valueOf(seatNos.size()));

        // 3. Create Booking with CONFIRMED status
        Booking booking = new Booking(bookingRef, passenger, tripId, totalAmount, BookingStatus.CONFIRMED);
        Booking savedBooking = bookingRepository.save(booking);

        // 4. Save BookingSeat records (with database unique constraint fail-safe)
        for (String seatNo : seatNos) {
            BookingSeat bookingSeat = new BookingSeat(savedBooking, trip.getBusId(), seatNo, tripId);
            bookingSeatRepository.save(bookingSeat);
            savedBooking.getSeats().add(bookingSeat);
        }

        // 5. Create Payment with COMPLETED status (BR-02)
        Payment payment = new Payment(savedBooking.getBookingId(), totalAmount, "CARD", PaymentStatus.COMPLETED, Instant.now());
        paymentRepository.save(payment);

        // 6. Release Redis locks
        seatLockService.releaseSeats(tripId, seatNos, holdToken);

        return savedBooking;
    }

    /**
     * Cancels an existing booking and initiates the refund workflow (UC-09, UC-10).
     */
    @Transactional
    public Booking cancelBooking(String bookingRef) {
        Booking booking = bookingRepository.findByBookingRef(bookingRef)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found with reference: " + bookingRef));

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new IllegalStateException("Booking [" + bookingRef + "] is already cancelled.");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        // Initiate Refund if payment was completed
        paymentRepository.findByBookingId(booking.getBookingId()).ifPresent(payment -> {
            if (payment.getStatus() == PaymentStatus.COMPLETED) {
                if (refundRepository.findByPayment(payment).isEmpty()) {
                    Refund refund = new Refund(payment, booking.getTotalAmount(), "Passenger cancellation request for " + bookingRef, RefundStatus.PENDING);
                    refundRepository.save(refund);
                }
            }
        });

        return booking;
    }

    public Booking getBookingByRef(String bookingRef) {
        return bookingRepository.findByBookingRef(bookingRef)
                .orElseThrow(() -> new IllegalArgumentException("Booking with reference " + bookingRef + " not found."));
    }

    public Optional<Payment> getPaymentForBooking(Long bookingId) {
        return paymentRepository.findByBookingId(bookingId);
    }

    public Optional<Refund> getRefundForPayment(Long paymentId) {
        return refundRepository.findByPaymentPaymentId(paymentId);
    }

    public List<Trip> searchPublishedTrips(String origin, String destination, LocalDate date) {
        return tripScheduleService.getAllTrips().stream()
                .filter(t -> Boolean.TRUE.equals(t.getPublished()))
                .filter(t -> t.getStatus() != TripStatus.CANCELLED)
                .filter(t -> {
                    if (origin != null && !origin.isBlank()) {
                        if (!t.getRoute().getOrigin().toLowerCase().contains(origin.trim().toLowerCase())) {
                            return false;
                        }
                    }
                    if (destination != null && !destination.isBlank()) {
                        if (!t.getRoute().getDestination().toLowerCase().contains(destination.trim().toLowerCase())) {
                            return false;
                        }
                    }
                    if (date != null) {
                        return t.getTripDate().equals(date);
                    }
                    return true;
                })
                .toList();
    }
}
