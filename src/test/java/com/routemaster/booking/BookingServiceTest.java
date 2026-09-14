package com.routemaster.booking;

import com.routemaster.booking.entity.*;
import com.routemaster.booking.lock.SeatLockService;
import com.routemaster.booking.repository.*;
import com.routemaster.booking.service.BookingService;
import com.routemaster.fleet.service.BusService;
import com.routemaster.operations.entity.Route;
import com.routemaster.operations.entity.Trip;
import com.routemaster.operations.service.TripScheduleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private BookingSeatRepository bookingSeatRepository;

    @Mock
    private PassengerRepository passengerRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private RefundRepository refundRepository;

    @Mock
    private SeatLockService seatLockService;

    @Mock
    private TripScheduleService tripScheduleService;

    @Mock
    private BusService busService;

    private BookingService bookingService;

    @BeforeEach
    void setUp() {
        bookingService = new BookingService(
                bookingRepository,
                bookingSeatRepository,
                passengerRepository,
                paymentRepository,
                refundRepository,
                seatLockService,
                tripScheduleService,
                busService
        );
    }

    @Test
    @DisplayName("BR-02 & BR-03: Booking confirmation creates Passenger, Booking, Payment, and releases locks")
    void testConfirmBooking_Success() {
        Route route = new Route("Colombo-Kandy", "Colombo", "Kandy", BigDecimal.valueOf(115.0), 180);
        Trip trip = new Trip(route, LocalDate.now(), LocalTime.of(8, 0), LocalTime.of(11, 0));
        trip.setTripId(101L);
        trip.setBusId(5L);
        trip.setPublished(true);

        when(tripScheduleService.getTripById(101L)).thenReturn(trip);
        when(bookingSeatRepository.existsByTripIdAndBusIdAndSeatNo(eq(101L), eq(5L), anyString())).thenReturn(false);

        Passenger passenger = new Passenger("Kamal Silva", "199512345678", "0771234567", "kamal@gmail.com");
        when(passengerRepository.findByNic("199512345678")).thenReturn(Optional.of(passenger));
        when(passengerRepository.save(any(Passenger.class))).thenReturn(passenger);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Booking booking = bookingService.confirmBooking(
                101L,
                List.of("1A", "1B"),
                "Kamal Silva",
                "0771234567",
                "199512345678",
                "kamal@gmail.com",
                "test-token-123"
        );

        assertNotNull(booking);
        assertTrue(booking.getBookingRef().startsWith("RM-"), "Booking reference must follow BR-03 format");
        assertEquals(BookingStatus.CONFIRMED, booking.getStatus());
        assertEquals(new BigDecimal("3000.00"), booking.getTotalAmount());
        assertEquals("Kamal Silva", booking.getPassenger().getName());

        // Verify Payment creation with status COMPLETED (BR-02)
        verify(paymentRepository, times(1)).save(argThat(payment ->
                payment.getStatus() == PaymentStatus.COMPLETED &&
                        payment.getAmount().equals(new BigDecimal("3000.00"))
        ));

        // Verify seats persisted
        verify(bookingSeatRepository, times(2)).save(any(BookingSeat.class));

        // Verify Redis lock release
        verify(seatLockService, times(1)).releaseSeats(101L, List.of("1A", "1B"), "test-token-123");
    }

    @Test
    @DisplayName("Fail-safe: Double booking prevented if seat already exists in database")
    void testConfirmBooking_DuplicateSeatInDb_ThrowsException() {
        Route route = new Route("Colombo-Kandy", "Colombo", "Kandy", BigDecimal.valueOf(115.0), 180);
        Trip trip = new Trip(route, LocalDate.now(), LocalTime.of(8, 0), LocalTime.of(11, 0));
        trip.setTripId(102L);
        trip.setBusId(5L);
        trip.setPublished(true);

        when(tripScheduleService.getTripById(102L)).thenReturn(trip);
        when(bookingSeatRepository.existsByTripIdAndBusIdAndSeatNo(102L, 5L, "2A")).thenReturn(true);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                bookingService.confirmBooking(
                        102L,
                        List.of("2A"),
                        "Nimal Perera",
                        "0719876543",
                        "200112345678",
                        null,
                        "token-abc"
                )
        );

        assertTrue(ex.getMessage().contains("Fail-safe Alert"));
        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("UC-09 & UC-10: Booking cancellation updates status to CANCELLED and creates PENDING refund")
    void testCancelBooking_CreatesPendingRefund() {
        Passenger passenger = new Passenger("Anura Silva", "199012345678", "0770001122", "anura@sliit.lk");
        Booking booking = new Booking("RM-TEST1234", passenger, 103L, new BigDecimal("1500.00"), BookingStatus.CONFIRMED);
        booking.setBookingId(55L);

        Payment payment = new Payment(55L, new BigDecimal("1500.00"), "CARD", PaymentStatus.COMPLETED, null);
        payment.setPaymentId(88L);

        when(bookingRepository.findByBookingRef("RM-TEST1234")).thenReturn(Optional.of(booking));
        when(paymentRepository.findByBookingId(55L)).thenReturn(Optional.of(payment));
        when(refundRepository.findByPayment(payment)).thenReturn(Optional.empty());

        Booking cancelled = bookingService.cancelBooking("RM-TEST1234");

        assertEquals(BookingStatus.CANCELLED, cancelled.getStatus());
        verify(bookingRepository, times(1)).save(booking);

        // Verify Refund record generated with PENDING status
        verify(refundRepository, times(1)).save(argThat(refund ->
                refund.getStatus() == RefundStatus.PENDING &&
                        refund.getRefundAmount().equals(new BigDecimal("1500.00"))
        ));
    }
}
