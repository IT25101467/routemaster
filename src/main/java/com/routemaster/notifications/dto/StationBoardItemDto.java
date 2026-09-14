package com.routemaster.notifications.dto;

import com.routemaster.operations.entity.TripStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Departure entry on the Public Live Station Board.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StationBoardItemDto {

    private Long tripId;
    private String routeName;
    private String origin;
    private String destination;
    private LocalDate tripDate;
    private LocalTime departureTime;
    private LocalTime arrivalTime;
    private String platform;
    private TripStatus status;
    private Integer delayMinutes;
    private LocalTime revisedDeparture;
    private String coachRegNo;
}
