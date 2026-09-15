package com.cosre.cosre_backend.modules.incident.event;
import java.time.LocalDateTime;
/** Cong Duy: consume with @TransactionalEventListener(AFTER_COMMIT) and email active Admins. */
public record IncidentReportedEvent(Long reportId, Long reporterId, String title, LocalDateTime createdAt) {}
