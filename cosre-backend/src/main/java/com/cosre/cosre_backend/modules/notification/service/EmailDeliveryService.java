package com.cosre.cosre_backend.modules.notification.service;

import com.cosre.cosre_backend.modules.account.entity.User;
import com.cosre.cosre_backend.modules.notification.entity.EmailDelivery;
import com.cosre.cosre_backend.modules.notification.entity.Notification;
import com.cosre.cosre_backend.modules.notification.repository.EmailDeliveryRepository;
import com.cosre.cosre_backend.modules.notification.repository.NotificationRepository;
import com.cosre.cosre_backend.modules.account.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service @Transactional
public class EmailDeliveryService {
    private final EmailDeliveryRepository repository; private final JavaMailSender sender; private final boolean enabled; private final UserRepository users; private final NotificationRepository notifications;
    /** Mail is optional in local development; requiring an SMTP bean must not prevent the API from starting. */
    public EmailDeliveryService(EmailDeliveryRepository repository, ObjectProvider<JavaMailSender> senderProvider, UserRepository users, NotificationRepository notifications, @Value("${app.mail.enabled:false}") boolean enabled) { this.repository = repository; this.sender = senderProvider.getIfAvailable(); this.users = users; this.notifications = notifications; this.enabled = enabled; }
    public void enqueue(User user, Notification notification, String eventKey) {
        if (eventKey == null || repository.existsByUserIdAndEventKey(user.getId(), eventKey)) return;
        EmailDelivery value = new EmailDelivery(); value.setUserId(user.getId()); value.setNotificationId(notification.getId()); value.setEventKey(eventKey);
        value.setStatus(enabled ? "PENDING" : "DISABLED"); value.setNextAttemptAt(LocalDateTime.now()); repository.save(value);
        if (enabled) deliver(value, user, notification);
    }
    @Scheduled(fixedDelayString = "${app.mail.retry-check-ms:60000}") public void retry() { if (!enabled) return; for (EmailDelivery value : repository.findTop100ByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc("PENDING", LocalDateTime.now())) users.findById(value.getUserId()).flatMap(user -> notifications.findById(value.getNotificationId()).map(notification -> new Object[]{user, notification})).ifPresent(pair -> deliver(value, (User) pair[0], (Notification) pair[1])); }
    private void deliver(EmailDelivery value, User user, Notification notification) {
        try { if (sender == null) throw new IllegalStateException("SMTP is not configured"); SimpleMailMessage message = new SimpleMailMessage(); message.setTo(user.getEmail()); message.setSubject(notification.getTitle()); message.setText(notification.getMessage()); sender.send(message); value.setStatus("SENT"); value.setSentAt(LocalDateTime.now()); }
        catch (RuntimeException ex) { int attempts = value.getAttempts() + 1; value.setAttempts(attempts); value.setLastError(ex.getMessage()); value.setNextAttemptAt(attempts >= 5 ? null : LocalDateTime.now().plusMinutes(1L << Math.min(attempts, 5))); if (attempts >= 5) value.setStatus("FAILED"); }
        repository.save(value);
    }
}
