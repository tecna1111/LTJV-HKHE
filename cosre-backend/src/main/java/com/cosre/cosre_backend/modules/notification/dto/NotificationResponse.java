package com.cosre.cosre_backend.modules.notification.dto;

import com.cosre.cosre_backend.modules.notification.entity.Notification;
import java.time.LocalDateTime;

public record NotificationResponse(Long id, String type, String title, String message,
        String link, boolean read, LocalDateTime createdAt) {
    public static NotificationResponse from(Notification value) {
        return new NotificationResponse(value.getId(), value.getType(), value.getTitle(), value.getMessage(),
                value.getLink(), value.isRead(), value.getCreatedAt());
    }
}
