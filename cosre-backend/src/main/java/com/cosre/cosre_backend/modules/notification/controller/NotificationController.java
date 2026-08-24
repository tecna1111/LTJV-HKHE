package com.cosre.cosre_backend.modules.notification.controller;

import com.cosre.cosre_backend.common.dto.ApiResponse;
import com.cosre.cosre_backend.modules.notification.dto.NotificationResponse;
import com.cosre.cosre_backend.modules.notification.dto.NotificationSummaryResponse;
import com.cosre.cosre_backend.modules.notification.service.NotificationService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {
    private final NotificationService service;
    public NotificationController(NotificationService service) { this.service = service; }

    @GetMapping
    public ApiResponse<NotificationSummaryResponse> list(Authentication auth) {
        return new ApiResponse<>(true, "Notifications loaded", service.list(auth.getName()));
    }

    @PutMapping("/{id}/read")
    public ApiResponse<NotificationResponse> read(@PathVariable Long id, Authentication auth) {
        return new ApiResponse<>(true, "Notification marked as read", service.markRead(id, auth.getName()));
    }

    @PutMapping("/read-all")
    public ApiResponse<Void> readAll(Authentication auth) {
        service.markAllRead(auth.getName());
        return new ApiResponse<>(true, "Notifications marked as read", null);
    }
}
