package com.routemaster.booking.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class SeatHoldRequest {
    private Long tripId;
    private Long busId;
    private List<String> seatNos;
    private String holdToken;
}
