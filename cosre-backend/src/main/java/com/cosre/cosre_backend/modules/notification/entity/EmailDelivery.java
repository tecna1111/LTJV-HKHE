package com.cosre.cosre_backend.modules.notification.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notification_email_deliveries", uniqueConstraints = @UniqueConstraint(name = "uk_notification_email_delivery_event", columnNames = {"user_id", "event_key"}))
public class EmailDelivery {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "notification_id", nullable = false) private Long notificationId;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(name = "event_key", nullable = false, length = 180) private String eventKey;
    @Column(nullable = false, length = 20) private String status;
    @Column(nullable = false) private int attempts;
    @Column(name = "next_attempt_at") private LocalDateTime nextAttemptAt;
    @Column(name = "last_error", length = 500) private String lastError;
    @Column(name = "sent_at") private LocalDateTime sentAt;
    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @PrePersist void created() { if (createdAt == null) createdAt = LocalDateTime.now(); }
    public Long getId() { return id; } public Long getNotificationId() { return notificationId; } public void setNotificationId(Long v) { notificationId = v; }
    public Long getUserId() { return userId; } public void setUserId(Long v) { userId = v; } public String getEventKey() { return eventKey; } public void setEventKey(String v) { eventKey = v; }
    public String getStatus() { return status; } public void setStatus(String v) { status = v; } public int getAttempts() { return attempts; } public void setAttempts(int v) { attempts = v; }
    public LocalDateTime getNextAttemptAt() { return nextAttemptAt; } public void setNextAttemptAt(LocalDateTime v) { nextAttemptAt = v; } public void setLastError(String v) { lastError = v; } public void setSentAt(LocalDateTime v) { sentAt = v; }
}
