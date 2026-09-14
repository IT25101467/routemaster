package com.routemaster.booking.controller;

import com.routemaster.booking.dto.SeatHoldRequest;
import com.routemaster.booking.dto.SeatHoldResponse;
import com.routemaster.booking.dto.SeatMapItemDto;
import com.routemaster.booking.entity.Booking;
import com.routemaster.booking.entity.Payment;
import com.routemaster.booking.entity.Refund;
import com.routemaster.booking.lock.SeatLockService;
import com.routemaster.booking.service.BookingService;
import com.routemaster.fleet.entity.Bus;
import com.routemaster.fleet.service.BusService;
import com.routemaster.operations.entity.Trip;
import com.routemaster.operations.service.TripScheduleService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Controller
@RequestMapping("/booking")
public class BookingController {

    private final BookingService bookingService;
    private final SeatLockService seatLockService;
    private final TripScheduleService tripScheduleService;
    private final BusService busService;

    public BookingController(BookingService bookingService,
                             SeatLockService seatLockService,
                             TripScheduleService tripScheduleService,
                             BusService busService) {
        this.bookingService = bookingService;
        this.seatLockService = seatLockService;
        this.tripScheduleService = tripScheduleService;
        this.busService = busService;
    }

    /**
     * Search published trips by origin, destination, and departure date.
     */
    @GetMapping("/search")
    public String searchTrips(@RequestParam(value = "origin", required = false) String origin,
                              @RequestParam(value = "destination", required = false) String destination,
                              @RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                              Model model) {
        List<Trip> trips = bookingService.searchPublishedTrips(origin, destination, date);

        model.addAttribute("trips", trips);
        model.addAttribute("origin", origin);
        model.addAttribute("destination", destination);
        model.addAttribute("date", date != null ? date : LocalDate.now());

        return "booking/search";
    }

    /**
     * Interactive seat layout rendering (2-column layout).
     * Seats marked as Available (green), Locked (yellow with timer), or Booked (gray disabled).
     */
    @GetMapping("/trips/{tripId}/seatmap")
    public String viewSeatMap(@PathVariable("tripId") Long tripId, Model model) {
        Trip trip = tripScheduleService.getTripById(tripId);
        if (trip.getBusId() == null) {
            model.addAttribute("errorMessage", "This trip does not yet have a bus assigned.");
            return "redirect:/booking/search";
        }

        Bus bus = busService.getBusById(trip.getBusId());
        List<SeatMapItemDto> seatMap = bookingService.getSeatMap(tripId);
        String holdToken = UUID.randomUUID().toString();

        model.addAttribute("trip", trip);
        model.addAttribute("bus", bus);
        model.addAttribute("seatMap", seatMap);
        model.addAttribute("holdToken", holdToken);

        return "booking/seatmap";
    }

    /**
     * AJAX endpoint to hold seats in Redisson distributed cache with 300s TTL.
     */
    @PostMapping("/hold")
    @ResponseBody
    public ResponseEntity<SeatHoldResponse> holdSeats(@RequestBody SeatHoldRequest request) {
        try {
            seatLockService.holdSeats(request.getTripId(), request.getBusId(), request.getSeatNos(), request.getHoldToken());
            return ResponseEntity.ok(new SeatHoldResponse(
                    true,
                    "Seats reserved for 5 minutes (300 seconds). Please complete passenger details and checkout.",
                    request.getHoldToken(),
                    300,
                    request.getSeatNos()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new SeatHoldResponse(
                    false,
                    e.getMessage(),
                    request.getHoldToken(),
                    0,
                    List.of()
            ));
        }
    }

    /**
     * Confirms the booking transaction (BR-02, BR-03):
     * Persists Booking, BookingSeat, and Payment, then releases Redis locks.
     */
    @PostMapping("/confirm")
    public String confirmBooking(@RequestParam("tripId") Long tripId,
                                 @RequestParam("seatNos") List<String> seatNos,
                                 @RequestParam("passengerName") String passengerName,
                                 @RequestParam("phone") String phone,
                                 @RequestParam("nic") String nic,
                                 @RequestParam(value = "email", required = false) String email,
                                 @RequestParam("holdToken") String holdToken,
                                 RedirectAttributes redirectAttributes) {
        try {
            Booking booking = bookingService.confirmBooking(tripId, seatNos, passengerName, phone, nic, email, holdToken);
            redirectAttributes.addFlashAttribute("successMessage", "Reservation confirmed successfully! Reference: " + booking.getBookingRef());
            return "redirect:/booking/ticket/" + booking.getBookingRef();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Booking failed: " + e.getMessage());
            return "redirect:/booking/trips/" + tripId + "/seatmap";
        }
    }

    /**
     * Displays confirmation e-ticket with boarding pass formatting.
     */
    @GetMapping("/ticket/{bookingRef}")
    public String viewTicket(@PathVariable("bookingRef") String bookingRef, Model model) {
        Booking booking = bookingService.getBookingByRef(bookingRef);
        Trip trip = tripScheduleService.getTripById(booking.getTripId());
        Bus bus = trip.getBusId() != null ? busService.getBusById(trip.getBusId()) : null;
        Optional<Payment> payment = bookingService.getPaymentForBooking(booking.getBookingId());
        Optional<Refund> refund = payment.flatMap(p -> bookingService.getRefundForPayment(p.getPaymentId()));

        model.addAttribute("booking", booking);
        model.addAttribute("trip", trip);
        model.addAttribute("bus", bus);
        model.addAttribute("payment", payment.orElse(null));
        model.addAttribute("refund", refund.orElse(null));

        return "booking/ticket";
    }

    /**
     * Cancels an existing booking and initiates the refund workflow (UC-09, UC-10).
     */
    @PostMapping("/ticket/{bookingRef}/cancel")
    public String cancelBooking(@PathVariable("bookingRef") String bookingRef, RedirectAttributes redirectAttributes) {
        try {
            bookingService.cancelBooking(bookingRef);
            redirectAttributes.addFlashAttribute("successMessage", "Booking " + bookingRef + " has been cancelled. Refund request initiated (Status: PENDING).");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Cancellation failed: " + e.getMessage());
        }
        return "redirect:/booking/ticket/" + bookingRef;
    }
}
