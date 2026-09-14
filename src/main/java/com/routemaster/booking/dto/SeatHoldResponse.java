package com.routemaster.booking.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SeatHoldResponse {
    private boolean success;
    private String message;
    private String holdToken;
    private long ttlSeconds;
    private List<String> heldSeats;
}
