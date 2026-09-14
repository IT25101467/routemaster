package com.routemaster.finance.controller;

import com.routemaster.finance.dto.FareForm;
import com.routemaster.finance.dto.FinanceDashboardDto;
import com.routemaster.finance.entity.Fare;
import com.routemaster.finance.service.FinanceService;
import com.routemaster.operations.repository.RouteRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Controller for Financial Analytics and Fare Management (Module F5 - Ranasinghe V.N. / IT25102675).
 * Provides executive KPI reporting, dynamic pricing management, and CSV audit streaming.
 */
@Controller
@RequestMapping("/finance")
public class FinanceController {

    private final FinanceService financeService;
    private final RouteRepository routeRepository;

    public FinanceController(FinanceService financeService, RouteRepository routeRepository) {
        this.financeService = financeService;
        this.routeRepository = routeRepository;
    }

    /**
     * Financial analytics and revenue intelligence dashboard.
     */
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        FinanceDashboardDto dashboard = financeService.getDashboardData();
        model.addAttribute("dashboard", dashboard);
        model.addAttribute("routes", routeRepository.findAll());

        if (!model.containsAttribute("fareForm")) {
            model.addAttribute("fareForm", FareForm.builder()
                    .effectiveFrom(LocalDate.now())
                    .surgeMultiplier(BigDecimal.valueOf(1.0))
                    .isActive(true)
                    .build());
        }

        return "finance/dashboard";
    }

    /**
     * Dedicated Apple HIG Fares & Surge Pricing Management Screen.
     */
    @GetMapping("/fares")
    public String faresManagement(Model model) {
        List<Fare> fares = financeService.getAllFares();
        model.addAttribute("fares", fares);
        model.addAttribute("routes", routeRepository.findAll());

        long activeCount = fares.stream().filter(f -> Boolean.TRUE.equals(f.getIsActive())).count();
        long surgeCount = fares.stream()
                .filter(f -> f.getSurgeMultiplier() != null && f.getSurgeMultiplier().compareTo(BigDecimal.ONE) > 0)
                .count();

        model.addAttribute("totalActiveFares", activeCount);
        model.addAttribute("totalSurgeRoutes", surgeCount);

        if (!model.containsAttribute("fareForm")) {
            model.addAttribute("fareForm", FareForm.builder()
                    .effectiveFrom(LocalDate.now())
                    .surgeMultiplier(BigDecimal.valueOf(1.0))
                    .isActive(true)
                    .build());
        }

        return "finance/fares-management";
    }

    /**
     * Creates a new route fare definition.
     */
    @PostMapping("/fares")
    public String createFare(@Valid @ModelAttribute("fareForm") FareForm form,
                             BindingResult bindingResult,
                             RedirectAttributes redirectAttributes,
                             Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("fares", financeService.getAllFares());
            model.addAttribute("routes", routeRepository.findAll());
            return "finance/fares-management";
        }

        try {
            financeService.createFare(form);
            redirectAttributes.addFlashAttribute("successMessage", "Fare policy registered successfully for Route #" + form.getRouteId());
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error saving fare: " + ex.getMessage());
        }

        return "redirect:/finance/fares";
    }

    /**
     * Updates an existing route fare definition (supports POST form submission).
     */
    @PostMapping("/fares/{id}/edit")
    public String updateFare(@PathVariable("id") Long id,
                             @Valid @ModelAttribute("fareForm") FareForm form,
                             BindingResult bindingResult,
                             RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Validation failed. Please review the input fields.");
            return "redirect:/finance/fares";
        }

        try {
            financeService.updateFare(id, form);
            redirectAttributes.addFlashAttribute("successMessage", "Fare policy #" + id + " updated successfully.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating fare: " + ex.getMessage());
        }

        return "redirect:/finance/fares";
    }

    /**
     * REST endpoint for updating a fare.
     */
    @PutMapping("/fares/{id}")
    public String updateFareRest(@PathVariable("id") Long id,
                                 @Valid @ModelAttribute("fareForm") FareForm form,
                                 BindingResult bindingResult,
                                 RedirectAttributes redirectAttributes) {
        return updateFare(id, form, bindingResult, redirectAttributes);
    }

    /**
     * Deletes a route fare policy (supports POST form submission).
     */
    @PostMapping("/fares/{id}/delete")
    public String deleteFare(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            financeService.deleteFare(id);
            redirectAttributes.addFlashAttribute("successMessage", "Fare policy #" + id + " deleted successfully.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting fare: " + ex.getMessage());
        }

        return "redirect:/finance/fares";
    }

    /**
     * REST endpoint for deleting a fare.
     */
    @DeleteMapping("/fares/{id}")
    public String deleteFareRest(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        return deleteFare(id, redirectAttributes);
    }

    /**
     * Generates and downloads financial audit CSV report.
     */
    @GetMapping("/reports/export/csv")
    public ResponseEntity<byte[]> exportCsv() {
        byte[] csvData = financeService.generateFinancialCsvReport();
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String filename = "RouteMaster_Financial_Report_" + dateStr + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csvData);
    }
}
