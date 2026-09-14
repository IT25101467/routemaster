package com.routemaster.operations.controller;

import com.routemaster.fleet.entity.Bus;
import com.routemaster.fleet.entity.BusStatus;
import com.routemaster.fleet.service.BusService;
import com.routemaster.operations.dto.DelayResponse;
import com.routemaster.operations.entity.DelayNotice;
import com.routemaster.operations.entity.Route;
import com.routemaster.operations.entity.Trip;
import com.routemaster.operations.entity.TripStatus;
import com.routemaster.operations.service.TripScheduleService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/operations")
public class ScheduleController {

    private final TripScheduleService tripScheduleService;
    private final BusService busService;

    public ScheduleController(TripScheduleService tripScheduleService, BusService busService) {
        this.tripScheduleService = tripScheduleService;
        this.busService = busService;
    }

    /**
     * Dashboard: List all trips, routes, fleet status, and quick-action modals.
     */
    @GetMapping("/trips")
    public String listTrips(Model model) {
        List<Trip> trips = tripScheduleService.getAllTrips();
        List<Route> routes = tripScheduleService.getAllRoutes();
        List<Bus> allBuses = busService.getAllBuses();

        List<Bus> activeBuses = allBuses.stream()
                .filter(b -> b.getStatus() == BusStatus.ACTIVE)
                .toList();

        Map<Long, Bus> busMap = allBuses.stream()
                .filter(b -> b.getBusId() != null)
                .collect(Collectors.toMap(Bus::getBusId, Function.identity(), (existing, replacing) -> existing));

        Map<Long, DelayNotice> delayNoticeMap = trips.stream()
                .filter(t -> t.getStatus() == TripStatus.DELAYED)
                .map(t -> tripScheduleService.getLatestDelayNotice(t.getTripId()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toMap(DelayNotice::getTripId, Function.identity(), (a, b) -> a));

        model.addAttribute("trips", trips);
        model.addAttribute("routes", routes);
        model.addAttribute("buses", activeBuses);
        model.addAttribute("busMap", busMap);
        model.addAttribute("delayNoticeMap", delayNoticeMap);

        if (!model.containsAttribute("routeForm")) {
            model.addAttribute("routeForm", new Route());
        }

        return "operations/trip-list";
    }

    /**
     * Register a new transit route enforcing BR-08 (origin != destination).
     */
    @PostMapping("/routes")
    public String createRoute(@Valid @ModelAttribute("routeForm") Route route,
                              BindingResult bindingResult,
                              RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.routeForm", bindingResult);
            redirectAttributes.addFlashAttribute("routeForm", route);
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to register route. Please check input errors.");
            return "redirect:/operations/trips";
        }

        try {
            tripScheduleService.createRoute(route);
            redirectAttributes.addFlashAttribute("successMessage", "Route '" + route.getRouteName() + "' created successfully (BR-08 passed).");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/operations/trips";
    }

    /**
     * Schedule a new trip on a route.
     */
    @PostMapping("/trips")
    public String scheduleTrip(@RequestParam("routeId") Long routeId,
                               @RequestParam("tripDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tripDate,
                               @RequestParam("departureTime") @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime departureTime,
                               @RequestParam(value = "durationMins", required = false) Integer durationMins,
                               RedirectAttributes redirectAttributes) {
        try {
            Trip trip = tripScheduleService.scheduleTrip(routeId, tripDate, departureTime, durationMins);
            redirectAttributes.addFlashAttribute("successMessage", "Trip #" + trip.getTripId() + " scheduled for " + tripDate + " " + departureTime + ".");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to schedule trip: " + e.getMessage());
        }
        return "redirect:/operations/trips";
    }

    /**
     * Assign Bus and Driver enforcing BR-05 (Active only), BR-06/BR-07 (Overlap + 30m turnaround),
     * and BR-10 (Driver duty conflict).
     * Auto-publishes trip upon assignment (BR-09).
     */
    @PostMapping("/trips/{id}/assign")
    public String assignBusAndDriver(@PathVariable("id") Long tripId,
                                     @RequestParam("busId") Long busId,
                                     @RequestParam("driverId") Long driverId,
                                     RedirectAttributes redirectAttributes) {
        try {
            Trip trip = tripScheduleService.assignBusAndDriver(tripId, busId, driverId);
            redirectAttributes.addFlashAttribute("successMessage", "Trip #" + trip.getTripId() + " assigned to Bus #" + busId + " and Driver #" + driverId + ". Published status: " + trip.getPublished() + " (BR-09 satisfied).");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/operations/trips";
    }

    /**
     * Publish a Delay Notice.
     * Updates trip status to DELAYED.
     * If delay > 60 minutes, flags replacement bus requirement (BR-12).
     */
    @PostMapping("/trips/{id}/delay")
    public String publishDelay(@PathVariable("id") Long tripId,
                               @RequestParam("delayMinutes") Integer delayMinutes,
                               @RequestParam("reason") String reason,
                               RedirectAttributes redirectAttributes) {
        try {
            DelayResponse response = tripScheduleService.publishDelay(tripId, delayMinutes, reason);
            if (response.replacementRequired()) {
                redirectAttributes.addFlashAttribute("replacementAlert", response.message());
            } else {
                redirectAttributes.addFlashAttribute("successMessage", response.message());
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Could not publish delay: " + e.getMessage());
        }
        return "redirect:/operations/trips";
    }

    /**
     * Assign a replacement bus for disrupted trips (BR-12 & BR-13).
     * Only permitted if the trip is delayed by > 60 minutes and the new bus has capacity >= bookings.
     */
    @PostMapping("/trips/{id}/replace")
    public String assignReplacementBus(@PathVariable("id") Long tripId,
                                       @RequestParam("newBusId") Long newBusId,
                                       RedirectAttributes redirectAttributes) {
        try {
            Trip trip = tripScheduleService.assignReplacementBus(tripId, newBusId);
            redirectAttributes.addFlashAttribute("successMessage", "Replacement Bus #" + newBusId + 
                    " successfully assigned to Trip #" + trip.getTripId() + " (BR-12 & BR-13 fulfilled).");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Replacement bus assignment failed: " + e.getMessage());
        }
        return "redirect:/operations/trips";
    }
}
