package com.routemaster.support.controller;

import com.routemaster.support.dto.ComplaintForm;
import com.routemaster.support.dto.MaskedBookingDetailsDto;
import com.routemaster.support.entity.Complaint;
import com.routemaster.support.entity.ComplaintStatus;
import com.routemaster.support.service.ComplaintService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/support")
public class ComplaintController {

    private final ComplaintService complaintService;

    public ComplaintController(ComplaintService complaintService) {
        this.complaintService = complaintService;
    }

    /**
     * Public complaint submission form.
     */
    @GetMapping("/submit")
    public String showSubmitForm(Model model) {
        if (!model.containsAttribute("complaintForm")) {
            model.addAttribute("complaintForm", new ComplaintForm());
        }
        return "support/submit";
    }

    /**
     * Processes public complaint submission (BR-14).
     */
    @PostMapping("/submit")
    public String submitComplaint(@Valid @ModelAttribute("complaintForm") ComplaintForm form,
                                  BindingResult bindingResult,
                                  RedirectAttributes redirectAttributes,
                                  Model model) {
        if (bindingResult.hasErrors()) {
            return "support/submit";
        }

        try {
            Complaint created = complaintService.submitComplaint(form);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Your complaint has been logged successfully under Tracking Ref: " + created.getRefNo()
                            + ". Our support team will review your report shortly.");
            redirectAttributes.addFlashAttribute("refNo", created.getRefNo());
            return "redirect:/support/submit";
        } catch (Exception ex) {
            model.addAttribute("errorMessage", "Error submitting complaint: " + ex.getMessage());
            return "support/submit";
        }
    }

    /**
     * Officer management queue with status transition buttons and filters.
     */
    @GetMapping("/tickets")
    public String listTickets(@RequestParam(value = "status", required = false) String statusStr, Model model) {
        List<Complaint> tickets;
        if (statusStr != null && !statusStr.isBlank() && !statusStr.equalsIgnoreCase("ALL")) {
            try {
                ComplaintStatus status = ComplaintStatus.valueOf(statusStr.toUpperCase());
                tickets = complaintService.getComplaintsByStatus(status);
                model.addAttribute("selectedStatus", status.name());
            } catch (IllegalArgumentException ex) {
                tickets = complaintService.getAllComplaints();
                model.addAttribute("selectedStatus", "ALL");
            }
        } else {
            tickets = complaintService.getAllComplaints();
            model.addAttribute("selectedStatus", "ALL");
        }

        model.addAttribute("tickets", tickets);
        model.addAttribute("statuses", ComplaintStatus.values());
        return "support/ticket-list";
    }

    /**
     * Executes ticket status transition (BR-15 state machine).
     */
    @PostMapping("/tickets/{id}/transition")
    public String transitionTicket(@PathVariable("id") Long id,
                                   @RequestParam("targetStatus") ComplaintStatus targetStatus,
                                   @RequestParam(value = "officerName", required = false) String officerName,
                                   RedirectAttributes redirectAttributes) {
        try {
            Complaint updated = complaintService.transitionStatus(id, targetStatus, officerName);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Ticket #" + updated.getRefNo() + " transitioned to status " + updated.getStatus().name()
                            + (updated.getAssignedOfficer() != null ? " (Officer: " + updated.getAssignedOfficer() + ")" : ""));
        } catch (IllegalStateException ex) {
            // BR-15 State Machine violation caught cleanly
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to update ticket: " + ex.getMessage());
        }
        return "redirect:/support/tickets";
    }

    /**
     * Displays ticket details with zero-trust masked booking information (BR-16).
     */
    @GetMapping("/tickets/{id}/details")
    public String ticketDetails(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        Optional<Complaint> complaintOpt = complaintService.getComplaintById(id);
        if (complaintOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Complaint #" + id + " does not exist.");
            return "redirect:/support/tickets";
        }

        Complaint complaint = complaintOpt.get();
        model.addAttribute("complaint", complaint);

        if (complaint.getBookingRef() != null && !complaint.getBookingRef().isBlank()) {
            Optional<MaskedBookingDetailsDto> maskedBooking = complaintService.getMaskedBookingDetails(complaint.getBookingRef());
            maskedBooking.ifPresent(dto -> model.addAttribute("booking", dto));
        }

        return "support/ticket-detail";
    }
}
