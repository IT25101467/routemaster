package com.routemaster.notifications.controller;

import com.routemaster.notifications.dto.StationBoardItemDto;
import com.routemaster.notifications.dto.TokenVerificationResult;
import com.routemaster.notifications.service.NotificationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
@RequestMapping("/station-board")
public class StationBoardController {

    private final NotificationService notificationService;

    public StationBoardController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * Public unauthenticated real-time station departure board.
     */
    @GetMapping
    public String stationBoard(Model model) {
        List<StationBoardItemDto> departures = notificationService.getLiveStationBoard();
        List<com.routemaster.notifications.entity.Notification> systemBroadcasts = notificationService.getActiveSystemBroadcasts();
        model.addAttribute("departures", departures);
        model.addAttribute("systemBroadcasts", systemBroadcasts);
        model.addAttribute("currentTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        model.addAttribute("currentDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("EEE, dd MMM yyyy")));
        return "notifications/station-board";
    }

    /**
     * Digital Pass Scanner & Inspector Verification Screen.
     */
    @GetMapping("/verify")
    public String showVerifyPass(Model model) {
        return "notifications/verify-pass";
    }

    /**
     * Verifies digital boarding pass authenticity via SHA-256 token hash or bookingRef.
     */
    @PostMapping("/verify")
    public String verifyPass(@RequestParam("query") String query, Model model) {
        TokenVerificationResult result = notificationService.verifyPass(query);
        model.addAttribute("result", result);
        model.addAttribute("query", query);
        return "notifications/verify-pass";
    }
}
