package com.routemaster.booking.dto;

import com.routemaster.booking.entity.SeatState;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SeatMapItemDto {
    private String seatNo;
    private String seatType;
    private String position;
    private SeatState state; // AVAILABLE, LOCKED, BOOKED
    private long remainingTtlSeconds;
}
