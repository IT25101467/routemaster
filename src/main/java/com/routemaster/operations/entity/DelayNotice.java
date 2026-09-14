package com.routemaster.operations.entity;

import com.routemaster.common.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalTime;

@Entity
@Table(name = "delay_notice")
@Getter
@Setter
@NoArgsConstructor
public class DelayNotice extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "delay_id")
    private Long delayId;

    @NotNull(message = "Trip ID is mandatory")
    @Column(name = "trip_id", nullable = false)
    private Long tripId;

    @NotNull(message = "Delay minutes must be specified")
    @Min(value = 1, message = "Delay must be at least 1 minute")
    @Column(name = "delay_minutes", nullable = false)
    private Integer delayMinutes;

    @NotBlank(message = "Reason for delay is mandatory")
    @Column(nullable = false, length = 255)
    private String reason;

    @NotNull(message = "Revised departure time must be provided")
    @Column(name = "revised_departure", nullable = false)
    private LocalTime revisedDeparture;

    @NotNull
    @Column(name = "published_at", nullable = false)
    private Instant publishedAt;

    public DelayNotice(Long tripId, Integer delayMinutes, String reason, LocalTime revisedDeparture, Instant publishedAt) {
        this.tripId = tripId;
        this.delayMinutes = delayMinutes;
        this.reason = reason;
        this.revisedDeparture = revisedDeparture;
        this.publishedAt = publishedAt;
    }
}
