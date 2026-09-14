package com.cosre.cosre_backend.modules.notification.service;

import com.cosre.cosre_backend.modules.account.entity.User;
import com.cosre.cosre_backend.modules.account.repository.UserRepository;
import com.cosre.cosre_backend.modules.chat.entity.ChatRoomType;
import com.cosre.cosre_backend.modules.classroom.repository.ClassroomRepository;
import com.cosre.cosre_backend.modules.notification.entity.Notification;
import com.cosre.cosre_backend.modules.notification.repository.NotificationRepository;
import com.cosre.cosre_backend.modules.team.entity.Team;
import com.cosre.cosre_backend.modules.team.repository.TeamRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTests {
    @Mock NotificationRepository repository;
    @Mock UserRepository userRepository;
    @Mock TeamRepository teamRepository;
    @Mock ClassroomRepository classroomRepository;
    @Mock SimpMessagingTemplate messagingTemplate;

    @Test
    void listsNotificationsAndUnreadCountForAuthenticatedUser() {
        User user = user(2L, "student", "Sinh vien");
        Notification value = new Notification();
        value.setUserId(2L); value.setType("CHAT_MESSAGE"); value.setTitle("Tin nhắn"); value.setMessage("Hello");
        when(userRepository.findByUsername("student")).thenReturn(Optional.of(user));
        when(repository.countByUserIdAndReadFalse(2L)).thenReturn(1L);
        when(repository.findTop50ByUserIdOrderByCreatedAtDesc(2L)).thenReturn(List.of(value));

        var result = service().list("student");

        assertEquals(1L, result.unreadCount());
        assertEquals(1, result.notifications().size());
    }

    @Test
    void chatNotificationExcludesSenderAndPushesToOtherTeamMembers() {
        User lecturer = user(1L, "lecturer", "Giang vien");
        User sender = user(2L, "student1", "Sinh vien 1");
        User recipient = user(3L, "student2", "Sinh vien 2");
        Team team = new Team();
        team.setLecturer(lecturer);
        team.getMembers().add(sender);
        team.getMembers().add(recipient);
        when(teamRepository.findById(5L)).thenReturn(Optional.of(team));
        when(repository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service().notifyChat(ChatRoomType.TEAM, 5L, sender, "Xin chào nhóm");

        verify(messagingTemplate).convertAndSendToUser(eq("lecturer"), eq("/queue/notifications"), any());
        verify(messagingTemplate).convertAndSendToUser(eq("student2"), eq("/queue/notifications"), any());
        verify(messagingTemplate, never()).convertAndSendToUser(eq("student1"), anyString(), any());
        verify(repository, times(2)).save(any(Notification.class));
    }

    private NotificationService service() {
        return new NotificationService(repository, userRepository, teamRepository, classroomRepository, messagingTemplate);
    }

    private User user(Long id, String username, String name) {
        User value = new User(); value.setId(id); value.setUsername(username); value.setFullName(name); value.setActive(true);
        return value;
    }
}
