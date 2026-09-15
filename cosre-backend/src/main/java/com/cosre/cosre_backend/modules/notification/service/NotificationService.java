package com.cosre.cosre_backend.modules.notification.service;

import com.cosre.cosre_backend.common.exception.ResourceNotFoundException;
import com.cosre.cosre_backend.common.constants.RoleEnum;
import com.cosre.cosre_backend.modules.account.entity.User;
import com.cosre.cosre_backend.modules.account.repository.UserRepository;
import com.cosre.cosre_backend.modules.chat.entity.ChatRoomType;
import com.cosre.cosre_backend.modules.classroom.repository.ClassroomRepository;
import com.cosre.cosre_backend.modules.notification.dto.NotificationResponse;
import com.cosre.cosre_backend.modules.notification.dto.NotificationSummaryResponse;
import com.cosre.cosre_backend.modules.notification.entity.Notification;
import com.cosre.cosre_backend.modules.notification.repository.NotificationRepository;
import com.cosre.cosre_backend.modules.team.repository.TeamRepository;
import com.cosre.cosre_backend.modules.team.entity.Team;
import com.cosre.cosre_backend.modules.meeting.entity.Meeting;
import com.cosre.cosre_backend.modules.meeting.entity.MeetingStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.Set;

@Service
@Transactional
public class NotificationService {
    private final NotificationRepository repository;
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final ClassroomRepository classroomRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final EmailDeliveryService emailDeliveryService;

    public NotificationService(NotificationRepository repository, UserRepository userRepository,
            TeamRepository teamRepository, ClassroomRepository classroomRepository,
            SimpMessagingTemplate messagingTemplate, EmailDeliveryService emailDeliveryService) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.teamRepository = teamRepository;
        this.classroomRepository = classroomRepository;
        this.messagingTemplate = messagingTemplate;
        this.emailDeliveryService = emailDeliveryService;
    }

    @Transactional(readOnly = true)
    public NotificationSummaryResponse list(String username) {
        User user = requireUser(username);
        return new NotificationSummaryResponse(repository.countByUserIdAndReadFalse(user.getId()),
                repository.findTop50ByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                        .map(NotificationResponse::from).toList());
    }

    public NotificationResponse markRead(Long id, String username) {
        User user = requireUser(username);
        Notification value = repository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        value.setRead(true);
        return NotificationResponse.from(repository.save(value));
    }

    public void markAllRead(String username) {
        User user = requireUser(username);
        var unread = repository.findByUserIdAndReadFalse(user.getId());
        unread.forEach(value -> value.setRead(true));
        repository.saveAll(unread);
    }

    public void notifyChat(ChatRoomType roomType, Long roomId, User sender, String content) {
        Set<User> recipients = recipients(roomType, roomId);
        recipients.removeIf(user -> user.getId().equals(sender.getId()) || !user.isActive());
        String roomLabel = roomType == ChatRoomType.TEAM ? "nhóm" : "lớp";
        String link = roomType == ChatRoomType.TEAM ? "/messages?type=TEAM&id=" + roomId
                : "/messages?type=CLASSROOM&id=" + roomId;
        String preview = content.length() > 120 ? content.substring(0, 117) + "…" : content;
        for (User recipient : recipients) {
            Notification value = new Notification();
            value.setUserId(recipient.getId());
            value.setType("CHAT_MESSAGE");
            value.setTitle("Tin nhắn mới trong " + roomLabel);
            value.setMessage(sender.getFullName() + ": " + preview);
            value.setLink(link);
            NotificationResponse response = NotificationResponse.from(repository.save(value));
            emailDeliveryService.enqueue(recipient, value, "CHAT:" + roomType + ":" + roomId + ":" + sender.getId() + ":" + Integer.toHexString(content.hashCode()));
            messagingTemplate.convertAndSendToUser(recipient.getUsername(), "/queue/notifications", response);
        }
    }
    public void notifyProjectSubmitted(Long projectId, String projectTitle, User lecturer) {
        String lecturerName = lecturer.getFullName() == null || lecturer.getFullName().isBlank()
                ? lecturer.getUsername() : lecturer.getFullName();
        for (User recipient : userRepository.findByRoleAndIsActiveTrueOrderByFullName(RoleEnum.HEAD_DEPT)) {
            Notification value = new Notification();
            value.setUserId(recipient.getId());
            value.setType("PROJECT_SUBMITTED");
            value.setTitle("Dự án mới chờ phê duyệt");
            value.setMessage(lecturerName + " đã gửi dự án “" + projectTitle + "” để phê duyệt.");
            value.setLink("/workflow?status=PENDING&projectId=" + projectId);
            NotificationResponse response = NotificationResponse.from(repository.save(value));
            emailDeliveryService.enqueue(recipient, value, "PROJECT_SUBMITTED:" + projectId);
            messagingTemplate.convertAndSendToUser(recipient.getUsername(), "/queue/notifications", response);
        }
    }

    public void notifyMeeting(Team team, Meeting meeting, String eventType) {
        String title;
        String message;
        if ("MEETING_CANCELLED".equals(eventType)) { title = "Lịch họp đã hủy"; message = "Cuộc họp “" + meeting.getTitle() + "” đã bị hủy."; }
        else if ("MEETING_STARTED".equals(eventType)) { title = "Cuộc họp đang diễn ra"; message = "Cuộc họp “" + meeting.getTitle() + "” vừa bắt đầu. Tham gia ngay."; }
        else if ("MEETING_REMINDER".equals(eventType)) { title = "Nhắc lịch họp"; message = "Cuộc họp “" + meeting.getTitle() + "” sắp bắt đầu."; }
        else if ("MEETING_UPDATED".equals(eventType)) { title = "Lịch họp đã thay đổi"; message = "Cuộc họp “" + meeting.getTitle() + "” vừa được cập nhật."; }
        else { title = "Lịch họp mới"; message = "Bạn được mời tham gia cuộc họp “" + meeting.getTitle() + "”."; }
        Set<User> recipients = new LinkedHashSet<>(team.getMembers()); recipients.add(team.getLecturer());
        for (User recipient : recipients) {
            if (!recipient.isActive()) continue;
            // The scheduled time is part of the idempotency key: moving a meeting must allow a
            // fresh reminder while retries for the same schedule remain harmless.
            String key = eventType + ":" + meeting.getId() + ":" + meeting.getStartsAt();
            if (repository.existsByUserIdAndEventKey(recipient.getId(), key)) continue;
            Notification value = new Notification(); value.setUserId(recipient.getId()); value.setType(eventType);
            value.setTitle(title); value.setMessage(message); value.setLink("/meetings?teamId=" + meeting.getTeamId()); value.setEventKey(key);
            NotificationResponse response = NotificationResponse.from(repository.save(value));
            emailDeliveryService.enqueue(recipient, value, key);
            messagingTemplate.convertAndSendToUser(recipient.getUsername(), "/queue/notifications", response);
        }
    }

    public void notifyEvent(Set<User> recipients, String eventId, String type, String title, String message, String link) {
        publishEvent(recipients, eventId, type, title, message, link, true);
    }

    public void notifyInAppEvent(Set<User> recipients, String eventId, String type, String title, String message, String link) {
        publishEvent(recipients, eventId, type, title, message, link, false);
    }

    public void notifyTeamEvent(Long teamId, String actorUsername, String eventId, String type,
                                String title, String message, String link) {
        var team = teamRepository.findById(teamId).orElseThrow(() -> new ResourceNotFoundException("Team not found"));
        Set<User> recipients = new LinkedHashSet<>(team.getMembers());
        recipients.add(team.getLecturer());
        recipients.removeIf(user -> user.getUsername().equals(actorUsername));
        notifyInAppEvent(recipients, eventId, type, title, message, link);
    }

    public void notifyClassroomEvent(Long classroomId, String actorUsername, String eventId, String type,
                                     String title, String message, String link) {
        var classroom = classroomRepository.findDetailedById(classroomId)
                .orElseThrow(() -> new ResourceNotFoundException("Classroom not found"));
        Set<User> recipients = new LinkedHashSet<>(classroom.getLecturers());
        recipients.addAll(classroom.getStudents());
        recipients.removeIf(user -> user.getUsername().equals(actorUsername));
        notifyInAppEvent(recipients, eventId, type, title, message, link);
    }

    public void notifyUserEvent(Long userId, String eventId, String type, String title, String message, String link) {
        var user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        notifyInAppEvent(Set.of(user), eventId, type, title, message, link);
    }

    private void publishEvent(Set<User> recipients, String eventId, String type, String title,
                              String message, String link, boolean sendEmail) {
        if (eventId == null || eventId.isBlank()) throw new IllegalArgumentException("A stable notification event id is required");
        for (User recipient : recipients) {
            if (!recipient.isActive() || repository.existsByUserIdAndEventKey(recipient.getId(), eventId)) continue;
            Notification value = new Notification(); value.setUserId(recipient.getId()); value.setEventKey(eventId);
            value.setType(type); value.setTitle(title); value.setMessage(message); value.setLink(link);
            NotificationResponse response = NotificationResponse.from(repository.save(value));
            if (sendEmail) emailDeliveryService.enqueue(recipient, value, eventId);
            messagingTemplate.convertAndSendToUser(recipient.getUsername(), "/queue/notifications", response);
        }
    }

    private Set<User> recipients(ChatRoomType roomType, Long roomId) {
        Set<User> result = new LinkedHashSet<>();
        if (roomType == ChatRoomType.TEAM) {
            var team = teamRepository.findById(roomId)
                    .orElseThrow(() -> new ResourceNotFoundException("Team not found"));
            result.add(team.getLecturer());
            result.addAll(team.getMembers());
            return result;
        }
        var classroom = classroomRepository.findDetailedById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Classroom not found"));
        result.addAll(classroom.getLecturers());
        result.addAll(classroom.getStudents());
        return result;
    }

    private User requireUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
