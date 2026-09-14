package com.routemaster.notifications.dto;

import com.routemaster.notifications.entity.NotificationChannel;
import com.routemaster.notifications.entity.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BroadcastForm {

    @NotNull(message = "Alert type is required.")
    @Builder.Default
    private NotificationType alertType = NotificationType.SYSTEM_BROADCAST;

    @Builder.Default
    private String broadcastScope = "GLOBAL"; // "GLOBAL" or "TRIP"

    private Long tripId;

    @NotBlank(message = "Broadcast message content is required.")
    @Size(max = 1000, message = "Broadcast message cannot exceed 1000 characters.")
    private String message;

    @Builder.Default
    private NotificationChannel channel = NotificationChannel.IN_APP;
}
