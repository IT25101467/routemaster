package com.routemaster.fleet.controller;

import com.routemaster.fleet.entity.Bus;
import com.routemaster.fleet.entity.BusStatus;
import com.routemaster.fleet.entity.ServiceRecord;
import com.routemaster.fleet.service.BusService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/fleet/buses")
public class BusController {

    private final BusService busService;

    public BusController(BusService busService) {
        this.busService = busService;
    }

    /**
     * Lists all fleet vehicles and provides registration form backing object.
     */
    @GetMapping
    public String listBuses(Model model) {
        model.addAttribute("buses", busService.getAllBuses());
        if (!model.containsAttribute("bus")) {
            model.addAttribute("bus", new Bus());
        }
        return "fleet/bus-list";
    }

    /**
     * Standalone Bus Registration Form view.
     */
    @GetMapping("/new")
    public String newBusForm(Model model) {
        if (!model.containsAttribute("bus")) {
            model.addAttribute("bus", new Bus());
        }
        return "fleet/bus-form";
    }

    /**
     * Registers a new Bus and auto-generates seats based on seatCapacity.
     * Catches BR-04 duplicate registration number violations.
     */
    @PostMapping
    public String addBus(@Valid @ModelAttribute("bus") Bus bus,
                         BindingResult result,
                         RedirectAttributes redirectAttributes,
                         Model model) {
        if (result.hasErrors()) {
            model.addAttribute("buses", busService.getAllBuses());
            return "fleet/bus-list";
        }
        try {
            busService.registerBus(bus);
            redirectAttributes.addFlashAttribute("successMessage", 
                    "Bus [" + bus.getRegistrationNo() + "] registered successfully with " + bus.getSeatCapacity() + " generated seats (BR-04 satisfied).");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            redirectAttributes.addFlashAttribute("bus", bus);
            return "redirect:/fleet/buses";
        }
        return "redirect:/fleet/buses";
    }

    /**
     * Updates Bus operational status (ACTIVE, UNDER_MAINTENANCE, RETIRED).
     * Enforces BR-05a (preventing RETIRED to ACTIVE transition).
     */
    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable("id") Long id,
                               @RequestParam("status") BusStatus status,
                               RedirectAttributes redirectAttributes) {
        try {
            busService.updateBusStatus(id, status);
            redirectAttributes.addFlashAttribute("successMessage", "Bus status updated to " + status + ".");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/fleet/buses";
    }

    /**
     * View maintenance and service logs for a specific bus.
     */
    @GetMapping("/{id}/services")
    public String viewServiceRecords(@PathVariable("id") Long id, Model model) {
        Bus bus = busService.getBusById(id);
        List<ServiceRecord> records = busService.getServiceRecords(id);

        model.addAttribute("bus", bus);
        model.addAttribute("records", records);
        if (!model.containsAttribute("serviceRecord")) {
            model.addAttribute("serviceRecord", new ServiceRecord());
        }
        return "fleet/service-records";
    }

    /**
     * Add a new service maintenance log for a bus.
     */
    @PostMapping("/{id}/services")
    public String addServiceRecord(@PathVariable("id") Long id,
                                   @Valid @ModelAttribute("serviceRecord") ServiceRecord serviceRecord,
                                   BindingResult result,
                                   RedirectAttributes redirectAttributes,
                                   Model model) {
        if (result.hasErrors()) {
            Bus bus = busService.getBusById(id);
            List<ServiceRecord> records = busService.getServiceRecords(id);
            model.addAttribute("bus", bus);
            model.addAttribute("records", records);
            return "fleet/service-records";
        }

        try {
            busService.addServiceRecord(id, serviceRecord);
            redirectAttributes.addFlashAttribute("successMessage", "Service maintenance record recorded successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to add service record: " + e.getMessage());
        }
        return "redirect:/fleet/buses/" + id + "/services";
    }
}