package com.routemaster.notifications.entity;

import com.routemaster.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Passenger Notification Record (Module F6 - Chathmal P.D.D. / IT25100115).
 * Dispatched under Rule BR-11 (Passenger-scoped disruption alerts).
 */
@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long notificationId;

    @Column(name = "booking_ref", length = 64)
    private String bookingRef;

    @Column(name = "trip_id")
    private Long tripId;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", length = 32)
    @Builder.Default
    private NotificationType alertType = NotificationType.SYSTEM_BROADCAST;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    public NotificationType getAlertType() {
        return alertType != null ? alertType : NotificationType.SYSTEM_BROADCAST;
    }

    public Boolean getIsActive() {
        return isActive != null ? isActive : true;
    }

    @Column(name = "message", nullable = false, length = 1000)
    private String message;

    @Column(name = "sent_at", nullable = false)
    private Instant sentAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 32)
    @Builder.Default
    private NotificationChannel channel = NotificationChannel.IN_APP;
}
