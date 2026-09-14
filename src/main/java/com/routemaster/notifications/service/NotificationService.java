package com.routemaster.notifications.service;

import com.routemaster.booking.entity.Booking;
import com.routemaster.booking.entity.BookingSeat;
import com.routemaster.booking.entity.BookingStatus;
import com.routemaster.booking.repository.BookingRepository;
import com.routemaster.common.event.TripDelayedEvent;
import com.routemaster.fleet.entity.Bus;
import com.routemaster.fleet.repository.BusRepository;
import com.routemaster.notifications.dto.StationBoardItemDto;
import com.routemaster.notifications.dto.TokenVerificationResult;
import com.routemaster.notifications.entity.Notification;
import com.routemaster.notifications.entity.NotificationChannel;
import com.routemaster.notifications.entity.PassToken;
import com.routemaster.notifications.repository.NotificationRepository;
import com.routemaster.notifications.repository.PassTokenRepository;
import com.routemaster.operations.entity.DelayNotice;
import com.routemaster.operations.entity.Trip;
import com.routemaster.operations.entity.TripStatus;
import com.routemaster.operations.repository.DelayNoticeRepository;
import com.routemaster.operations.repository.TripRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Real-time notification and inspection verification engine (Module F6 - Chathmal P.D.D. / IT25100115).
 * Implements Observer Pattern for BR-11 (Passenger disruption dispatch) and SHA-256 digital pass verification.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private static final String SALT = "ROUTEMASTER_SHA256_SALT";

    private final NotificationRepository notificationRepository;
    private final PassTokenRepository passTokenRepository;
    private final BookingRepository bookingRepository;
    private final TripRepository tripRepository;
    private final DelayNoticeRepository delayNoticeRepository;
    private final BusRepository busRepository;

    public NotificationService(NotificationRepository notificationRepository,
                               PassTokenRepository passTokenRepository,
                               BookingRepository bookingRepository,
                               TripRepository tripRepository,
                               DelayNoticeRepository delayNoticeRepository,
                               BusRepository busRepository) {
        this.notificationRepository = notificationRepository;
        this.passTokenRepository = passTokenRepository;
        this.bookingRepository = bookingRepository;
        this.tripRepository = tripRepository;
        this.delayNoticeRepository = delayNoticeRepository;
        this.busRepository = busRepository;
    }

    /**
     * Observer Pattern Implementation:
     * Subscribes to TripDelayedEvent published by Module F3 (Operations).
     * Dispatches passenger-scoped notifications strictly to passengers on the affected trip (Rule BR-11).
     */
    @EventListener
    @Transactional
    public void onTripDelayed(TripDelayedEvent event) {
        log.info("Observer Pattern: Received TripDelayedEvent for Trip #{} ({} mins delay)",
                event.tripId(), event.delayMinutes());

        List<Booking> affectedBookings = bookingRepository.findByTripId(event.tripId());
        int dispatchedCount = 0;

        for (Booking b : affectedBookings) {
            if (b.getStatus() == BookingStatus.CONFIRMED) {
                String alertMessage = "DISRUPTION ALERT (Rule BR-11): Trip #" + event.tripId()
                        + " is delayed by " + event.delayMinutes() + " minutes. Reason: " + event.reason()
                        + ". Revised schedule has been updated on the Live Station Board.";

                Notification notification = Notification.builder()
                        .bookingRef(b.getBookingRef())
                        .tripId(event.tripId())
                        .alertType(com.routemaster.notifications.entity.NotificationType.DELAY)
                        .isActive(true)
                        .message(alertMessage)
                        .sentAt(Instant.now())
                        .channel(NotificationChannel.IN_APP)
                        .build();

                notificationRepository.save(notification);
                dispatchedCount++;
            }
        }

        log.info("Rule BR-11 Dispatched {} passenger-scoped notifications for Trip #{}", dispatchedCount, event.tripId());
    }

    /**
     * Dispatches an operational broadcast alert across system or trip passengers.
     */
    @Transactional
    public Notification createBroadcast(com.routemaster.notifications.dto.BroadcastForm form) {
        String scope = form.getBroadcastScope() != null ? form.getBroadcastScope().toUpperCase() : "GLOBAL";
        com.routemaster.notifications.entity.NotificationType type = form.getAlertType() != null
                ? form.getAlertType()
                : com.routemaster.notifications.entity.NotificationType.SYSTEM_BROADCAST;

        if ("TRIP".equals(scope) && form.getTripId() != null) {
            // 1. Create master trip alert record
            Notification tripAlert = Notification.builder()
                    .bookingRef("TRIP-" + form.getTripId())
                    .tripId(form.getTripId())
                    .alertType(type)
                    .isActive(true)
                    .message(form.getMessage().trim())
                    .sentAt(Instant.now())
                    .channel(form.getChannel() != null ? form.getChannel() : NotificationChannel.IN_APP)
                    .build();

            Notification saved = notificationRepository.save(tripAlert);

            // 2. Dispatch passenger-scoped copies to all confirmed ticket holders on this trip
            List<Booking> tripBookings = bookingRepository.findByTripId(form.getTripId());
            for (Booking b : tripBookings) {
                if (b.getStatus() == BookingStatus.CONFIRMED) {
                    Notification passengerNotice = Notification.builder()
                            .bookingRef(b.getBookingRef())
                            .tripId(form.getTripId())
                            .alertType(type)
                            .isActive(true)
                            .message(form.getMessage().trim())
                            .sentAt(Instant.now())
                            .channel(form.getChannel() != null ? form.getChannel() : NotificationChannel.IN_APP)
                            .build();
                    notificationRepository.save(passengerNotice);
                }
            }
            return saved;
        } else {
            // Global System Alert
            Notification globalAlert = Notification.builder()
                    .bookingRef("GLOBAL")
                    .tripId(null)
                    .alertType(type)
                    .isActive(true)
                    .message(form.getMessage().trim())
                    .sentAt(Instant.now())
                    .channel(form.getChannel() != null ? form.getChannel() : NotificationChannel.IN_APP)
                    .build();

            return notificationRepository.save(globalAlert);
        }
    }

    /**
     * Updates an alert broadcast message content.
     */
    @Transactional
    public Notification updateAlert(Long id, String newMessage) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found with ID: " + id));

        notification.setMessage(newMessage.trim());
        return notificationRepository.save(notification);
    }

    /**
     * Retracts an operational alert (Soft-delete / hides from station board and passenger screens).
     */
    @Transactional
    public void retractAlert(Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found with ID: " + id));

        notification.setIsActive(false);
        notificationRepository.save(notification);
    }

    /**
     * Fetches all active broadcasts for admin review.
     */
    @Transactional(readOnly = true)
    public List<Notification> getActiveBroadcasts() {
        return notificationRepository.findByIsActiveTrueOrderBySentAtDesc();
    }

    /**
     * Fetches all broadcast alerts (active and retracted) for full command center audit.
     */
    @Transactional(readOnly = true)
    public List<Notification> getAllAlerts() {
        return notificationRepository.findAllByOrderBySentAtDesc();
    }

    /**
     * Retrieves active system and emergency broadcasts for display on the Live Station Board marquee.
     */
    @Transactional(readOnly = true)
    public List<Notification> getActiveSystemBroadcasts() {
        return notificationRepository.findByAlertTypeInAndIsActiveTrueOrderBySentAtDesc(
                List.of(com.routemaster.notifications.entity.NotificationType.SYSTEM_BROADCAST,
                        com.routemaster.notifications.entity.NotificationType.EMERGENCY));
    }

    /**
     * Generates or retrieves an authentic SHA-256 digital pass token for a confirmed booking.
     */
    @Transactional
    public PassToken generatePassToken(String bookingRef) {
        Optional<PassToken> existing = passTokenRepository.findByBookingRef(bookingRef);
        if (existing.isPresent()) {
            return existing.get();
        }

        String hash = computeSha256(bookingRef + ":" + SALT);

        PassToken token = PassToken.builder()
                .bookingRef(bookingRef)
                .tokenHash(hash)
                .issuedAt(Instant.now())
                .build();

        return passTokenRepository.save(token);
    }

    /**
     * Validates ticket authenticity for ticket inspectors and conductors.
     */
    @Transactional(readOnly = true)
    public TokenVerificationResult verifyPass(String query) {
        if (query == null || query.isBlank()) {
            return TokenVerificationResult.builder()
                    .valid(false)
                    .message("Please provide a Booking Reference or Digital Token Hash.")
                    .build();
        }

        String trimmed = query.trim();

        // Check if token exists by token hash or bookingRef
        Optional<PassToken> tokenOpt = passTokenRepository.findByTokenHash(trimmed)
                .or(() -> passTokenRepository.findByBookingRef(trimmed));

        Optional<Booking> bookingOpt = bookingRepository.findByBookingRef(trimmed);

        if (tokenOpt.isEmpty() && bookingOpt.isEmpty()) {
            return TokenVerificationResult.builder()
                    .valid(false)
                    .message("INVALID PASS: No matching booking or cryptographic token signature found.")
                    .build();
        }

        Booking booking;
        if (bookingOpt.isPresent()) {
            booking = bookingOpt.get();
        } else {
            booking = bookingRepository.findByBookingRef(tokenOpt.get().getBookingRef())
                    .orElse(null);
        }

        if (booking == null) {
            return TokenVerificationResult.builder()
                    .valid(false)
                    .message("TAMPERED PASS: Digital token exists but booking record has been deleted or invalidated.")
                    .build();
        }

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            return TokenVerificationResult.builder()
                    .valid(false)
                    .bookingRef(booking.getBookingRef())
                    .passengerName(booking.getPassenger() != null ? booking.getPassenger().getName() : "N/A")
                    .nic(booking.getPassenger() != null ? booking.getPassenger().getNic() : "N/A")
                    .bookingStatus("CANCELLED")
                    .message("REVOKED TICKET: This booking was CANCELLED. Boarding is not permitted.")
                    .build();
        }

        List<String> seats = booking.getSeats() != null
                ? booking.getSeats().stream().map(BookingSeat::getSeatNo).collect(Collectors.toList())
                : Collections.emptyList();

        Optional<Trip> tripOpt = tripRepository.findById(booking.getTripId());
        String routeName = tripOpt.map(t -> t.getRoute() != null ? t.getRoute().getRouteName() : "Trip #" + t.getTripId()).orElse("N/A");
        String tripDate = tripOpt.map(t -> t.getTripDate().toString()).orElse("N/A");
        String depTime = tripOpt.map(t -> t.getDepartureTime().toString()).orElse("N/A");

        String expectedHash = computeSha256(booking.getBookingRef() + ":" + SALT);

        return TokenVerificationResult.builder()
                .valid(true)
                .message("VERIFIED AUTHENTIC: Digital Boarding Pass is valid and authorized for travel.")
                .bookingRef(booking.getBookingRef())
                .passengerName(booking.getPassenger() != null ? booking.getPassenger().getName() : "N/A")
                .nic(booking.getPassenger() != null ? booking.getPassenger().getNic() : "N/A")
                .seatNos(seats)
                .tripRoute(routeName)
                .tripDate(tripDate)
                .departureTime(depTime)
                .bookingStatus(booking.getStatus().name())
                .tokenHash(expectedHash)
                .build();
    }

    /**
     * Compiles real-time station departure board data for public display.
     */
    @Transactional(readOnly = true)
    public List<StationBoardItemDto> getLiveStationBoard() {
        List<Trip> trips = tripRepository.findByPublishedTrueOrderByTripDateAscDepartureTimeAsc();
        Map<Long, Bus> busMap = busRepository.findAll().stream()
                .collect(Collectors.toMap(Bus::getBusId, Function.identity(), (a, b) -> a));

        List<StationBoardItemDto> board = new ArrayList<>();

        for (Trip t : trips) {
            Optional<DelayNotice> delay = delayNoticeRepository.findFirstByTripIdOrderByPublishedAtDesc(t.getTripId());

            String regNo = (t.getBusId() != null && busMap.containsKey(t.getBusId()))
                    ? busMap.get(t.getBusId()).getRegistrationNo()
                    : "TBD";

            String platform = "Bay 0" + ((t.getTripId() % 4) + 1);

            board.add(StationBoardItemDto.builder()
                    .tripId(t.getTripId())
                    .routeName(t.getRoute() != null ? t.getRoute().getRouteName() : "Express Route")
                    .origin(t.getRoute() != null ? t.getRoute().getOrigin() : "-")
                    .destination(t.getRoute() != null ? t.getRoute().getDestination() : "-")
                    .tripDate(t.getTripDate())
                    .departureTime(t.getDepartureTime())
                    .arrivalTime(t.getArrivalTime())
                    .platform(platform)
                    .status(t.getStatus())
                    .delayMinutes(delay.map(DelayNotice::getDelayMinutes).orElse(null))
                    .revisedDeparture(delay.map(DelayNotice::getRevisedDeparture).orElse(null))
                    .coachRegNo(regNo)
                    .build());
        }

        return board;
    }

    private String computeSha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hashBytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException("SHA-256 algorithm not available", ex);
        }
    }
}
