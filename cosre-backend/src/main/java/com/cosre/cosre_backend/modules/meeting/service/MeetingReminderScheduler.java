package com.cosre.cosre_backend.modules.meeting.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MeetingReminderScheduler {
    private final MeetingService service; public MeetingReminderScheduler(MeetingService service) { this.service = service; }
    @Scheduled(fixedDelayString = "${app.meeting.reminder-check-ms:60000}") public void sendDueReminders() { service.sendDueReminders(); }
}
