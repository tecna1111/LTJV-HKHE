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

    public NotificationService(NotificationRepository repository, UserRepository userRepository,
            TeamRepository teamRepository, ClassroomRepository classroomRepository,
            SimpMessagingTemplate messagingTemplate) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.teamRepository = teamRepository;
        this.classroomRepository = classroomRepository;
        this.messagingTemplate = messagingTemplate;
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
