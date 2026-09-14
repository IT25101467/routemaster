package com.routemaster.support.service;

import com.routemaster.booking.entity.Booking;
import com.routemaster.booking.entity.BookingSeat;
import com.routemaster.booking.entity.Payment;
import com.routemaster.booking.repository.BookingRepository;
import com.routemaster.booking.repository.PaymentRepository;
import com.routemaster.support.dto.ComplaintForm;
import com.routemaster.support.dto.MaskedBookingDetailsDto;
import com.routemaster.support.entity.Complaint;
import com.routemaster.support.entity.ComplaintStatus;
import com.routemaster.support.repository.ComplaintRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service orchestrating Customer Support operations (Module F4 - Perera I.I.S. / IT25100685).
 * Enforces BR-14 (Unique UUID tracking), BR-15 (State Machine Gating), and BR-16 (Zero-Trust PII Masking).
 */
@Service
public class ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;

    public ComplaintService(ComplaintRepository complaintRepository,
                            BookingRepository bookingRepository,
                            PaymentRepository paymentRepository) {
        this.complaintRepository = complaintRepository;
        this.bookingRepository = bookingRepository;
        this.paymentRepository = paymentRepository;
    }

    /**
     * Registers a new passenger complaint with a unique tracking reference (BR-14).
     */
    @Transactional
    public Complaint submitComplaint(ComplaintForm form) {
        String uniqueRef = "CMP-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();

        Complaint complaint = Complaint.builder()
                .refNo(uniqueRef)
                .bookingRef(form.getBookingRef() != null && !form.getBookingRef().isBlank() ? form.getBookingRef().trim() : null)
                .passengerName(form.getPassengerName().trim())
                .contactNumber(form.getContactNumber().trim())
                .category(form.getCategory().trim())
                .description(form.getDescription().trim())
                .status(ComplaintStatus.OPEN)
                .build();

        return complaintRepository.save(complaint);
    }

    /**
     * Executes lifecycle state transition for a complaint.
     * Enforces Rule BR-15: Direct transition from OPEN to CLOSED is strictly rejected;
     * ticket must pass through IN_PROGRESS or ESCALATED first.
     */
    @Transactional
    public Complaint transitionStatus(Long complaintId, ComplaintStatus targetStatus, String officerName) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new IllegalArgumentException("Complaint #" + complaintId + " not found."));

        ComplaintStatus current = complaint.getStatus();

        // Enforce BR-15 State Machine Gate
        if (current == ComplaintStatus.OPEN && targetStatus == ComplaintStatus.CLOSED) {
            throw new IllegalStateException("Rule BR-15 Violation: A complaint in OPEN status cannot directly transition to CLOSED without first being investigated under IN_PROGRESS or ESCALATED.");
        }

        if (current == ComplaintStatus.OPEN && targetStatus == ComplaintStatus.RESOLVED) {
            throw new IllegalStateException("Rule BR-15 Violation: A complaint in OPEN status cannot directly transition to RESOLVED without first being set to IN_PROGRESS.");
        }

        // Apply officer assignment if supplied
        if (officerName != null && !officerName.isBlank()) {
            complaint.setAssignedOfficer(officerName.trim());
        }

        complaint.setStatus(targetStatus);
        return complaintRepository.save(complaint);
    }

    /**
     * Fetches booking information with payment card details completely omitted under Rule BR-16.
     */
    @Transactional(readOnly = true)
    public Optional<MaskedBookingDetailsDto> getMaskedBookingDetails(String bookingRef) {
        if (bookingRef == null || bookingRef.isBlank()) {
            return Optional.empty();
        }

        return bookingRepository.findByBookingRef(bookingRef.trim()).map(b -> {
            Optional<Payment> paymentOpt = paymentRepository.findByBookingId(b.getBookingId());

            List<String> seatNos = b.getSeats() != null
                    ? b.getSeats().stream().map(BookingSeat::getSeatNo).collect(Collectors.toList())
                    : Collections.emptyList();

            return MaskedBookingDetailsDto.builder()
                    .bookingRef(b.getBookingRef())
                    .passengerName(b.getPassenger() != null ? b.getPassenger().getName() : "N/A")
                    .passengerPhone(b.getPassenger() != null ? b.getPassenger().getPhone() : "N/A")
                    .passengerNic(b.getPassenger() != null ? b.getPassenger().getNic() : "N/A")
                    .passengerEmail(b.getPassenger() != null ? b.getPassenger().getEmail() : null)
                    .tripId(b.getTripId())
                    .seatNos(seatNos)
                    .totalAmount(b.getTotalAmount())
                    .bookingStatus(b.getStatus().name())
                    // Zero-Trust Payment Masking (BR-16): PAN, CVV, and full card details are omitted
                    .paymentMethod(paymentOpt.map(Payment::getMethod).orElse("N/A"))
                    .paymentStatus(paymentOpt.map(p -> p.getStatus().name()).orElse("N/A"))
                    .cardSecurityNotice("Sensitive payment card credentials (PAN, CVV, expiration) are completely omitted under Security Rule BR-16.")
                    .build();
        });
    }

    @Transactional(readOnly = true)
    public List<Complaint> getAllComplaints() {
        return complaintRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<Complaint> getComplaintsByStatus(ComplaintStatus status) {
        return complaintRepository.findByStatusOrderByCreatedAtDesc(status);
    }

    @Transactional(readOnly = true)
    public Optional<Complaint> getComplaintById(Long id) {
        return complaintRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Complaint> getComplaintByRefNo(String refNo) {
        return complaintRepository.findByRefNo(refNo);
    }
}
