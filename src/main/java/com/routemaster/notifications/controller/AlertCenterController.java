package com.routemaster.notifications.controller;

import com.routemaster.notifications.dto.BroadcastForm;
import com.routemaster.notifications.entity.Notification;
import com.routemaster.notifications.entity.NotificationType;
import com.routemaster.notifications.service.NotificationService;
import com.routemaster.operations.entity.Trip;
import com.routemaster.operations.repository.TripRepository;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Controller for the Operational Alert Broadcast Command Center (Module F6 - Chathmal P.D.D. / IT25100115).
 * Empowers station masters and operators to broadcast and retract live announcements, disruptions, and emergencies.
 */
@Controller
@RequestMapping("/notifications")
public class AlertCenterController {

    private final NotificationService notificationService;
    private final TripRepository tripRepository;

    public AlertCenterController(NotificationService notificationService, TripRepository tripRepository) {
        this.notificationService = notificationService;
        this.tripRepository = tripRepository;
    }

    /**
     * Admin Alert Command Center Dashboard.
     */
    @GetMapping("/alerts")
    public String alertCenter(Model model) {
        List<Notification> alerts = notificationService.getAllAlerts();
        List<Trip> publishedTrips = tripRepository.findByPublishedTrueOrderByTripDateAscDepartureTimeAsc();

        long activeCount = alerts.stream().filter(a -> Boolean.TRUE.equals(a.getIsActive())).count();
        long emergencyCount = alerts.stream()
                .filter(a -> Boolean.TRUE.equals(a.getIsActive()) && a.getAlertType() == NotificationType.EMERGENCY)
                .count();

        model.addAttribute("alerts", alerts);
        model.addAttribute("trips", publishedTrips);
        model.addAttribute("activeCount", activeCount);
        model.addAttribute("emergencyCount", emergencyCount);

        if (!model.containsAttribute("broadcastForm")) {
            model.addAttribute("broadcastForm", BroadcastForm.builder()
                    .alertType(NotificationType.SYSTEM_BROADCAST)
                    .broadcastScope("GLOBAL")
                    .build());
        }

        return "notifications/alert-center";
    }

    /**
     * Creates and dispatches a new global or trip-specific broadcast alert.
     */
    @PostMapping("/broadcast")
    public String createBroadcast(@Valid @ModelAttribute("broadcastForm") BroadcastForm form,
                                  BindingResult bindingResult,
                                  RedirectAttributes redirectAttributes,
                                  Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("alerts", notificationService.getAllAlerts());
            model.addAttribute("trips", tripRepository.findByPublishedTrueOrderByTripDateAscDepartureTimeAsc());
            return "notifications/alert-center";
        }

        try {
            notificationService.createBroadcast(form);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Alert broadcast successfully dispatched [" + form.getAlertType() + "].");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to dispatch broadcast: " + ex.getMessage());
        }

        return "redirect:/notifications/alerts";
    }

    /**
     * Updates an existing alert message.
     */
    @PostMapping("/{id}/edit")
    public String updateAlert(@PathVariable("id") Long id,
                              @RequestParam("message") String message,
                              RedirectAttributes redirectAttributes) {
        if (message == null || message.trim().isBlank()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Alert message cannot be blank.");
            return "redirect:/notifications/alerts";
        }

        try {
            notificationService.updateAlert(id, message);
            redirectAttributes.addFlashAttribute("successMessage", "Alert #" + id + " message successfully updated.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating alert: " + ex.getMessage());
        }

        return "redirect:/notifications/alerts";
    }

    /**
     * Retracts an operational alert (Soft-delete / immediate withdrawal from displays).
     */
    @PostMapping("/{id}/retract")
    public String retractAlert(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            notificationService.retractAlert(id);
            redirectAttributes.addFlashAttribute("successMessage", "Alert #" + id + " has been retracted and withdrawn from active displays.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error retracting alert: " + ex.getMessage());
        }

        return "redirect:/notifications/alerts";
    }
}
