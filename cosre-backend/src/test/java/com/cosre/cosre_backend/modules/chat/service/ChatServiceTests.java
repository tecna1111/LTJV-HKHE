package com.cosre.cosre_backend.modules.chat.service;

import com.cosre.cosre_backend.modules.account.entity.User;
import com.cosre.cosre_backend.modules.account.repository.UserRepository;
import com.cosre.cosre_backend.modules.chat.dto.SendChatMessageRequest;
import com.cosre.cosre_backend.modules.chat.entity.ChatMessage;
import com.cosre.cosre_backend.modules.chat.entity.ChatRoomType;
import com.cosre.cosre_backend.modules.chat.repository.ChatMessageRepository;
import com.cosre.cosre_backend.modules.notification.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTests {
    @Mock ChatMessageRepository repository;
    @Mock UserRepository userRepository;
    @Mock ChatRoomAccessService accessService;
    @Mock NotificationService notificationService;

    @Test
    void sendUsesAuthenticatedUserAndPersistsTrimmedContent() {
        User sender = user(7L, "sv001", "Nguyen Van An");
        when(userRepository.findByUsername("sv001")).thenReturn(Optional.of(sender));
        when(repository.save(any(ChatMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ChatService service = new ChatService(repository, userRepository, accessService, notificationService);

        var response = service.send(ChatRoomType.TEAM, 3L, "sv001", new SendChatMessageRequest("  Xin chao  "));

        verify(accessService).requireAccess("sv001", ChatRoomType.TEAM, 3L);
        assertEquals(7L, response.senderId());
        assertEquals("Xin chao", response.content());
        verify(notificationService).notifyChat(ChatRoomType.TEAM, 3L, sender, "Xin chao");
    }

    @Test
    void historyReturnsMessagesWithSenderDetails() {
        User sender = user(7L, "sv001", "Nguyen Van An");
        ChatMessage message = new ChatMessage();
        message.setRoomType("TEAM"); message.setRoomId(3L); message.setSenderId(7L);
        message.setContent("Hello"); message.setCreatedAt(LocalDateTime.now());
        when(repository.findByRoomTypeAndRoomIdOrderByCreatedAtAsc("TEAM", 3L)).thenReturn(List.of(message));
        when(userRepository.findById(7L)).thenReturn(Optional.of(sender));
        ChatService service = new ChatService(repository, userRepository, accessService, notificationService);

        var result = service.history(ChatRoomType.TEAM, 3L, "sv001");

        assertEquals(1, result.size());
        assertEquals("Nguyen Van An", result.get(0).senderName());
    }

    @Test
    void sendStopsWhenUserCannotAccessRoom() {
        doThrow(new AccessDeniedException("denied")).when(accessService)
                .requireAccess("outsider", ChatRoomType.TEAM, 3L);
        ChatService service = new ChatService(repository, userRepository, accessService, notificationService);

        assertThrows(AccessDeniedException.class,
                () -> service.send(ChatRoomType.TEAM, 3L, "outsider", new SendChatMessageRequest("Hello")));
        verifyNoInteractions(repository);
    }

    private User user(Long id, String username, String fullName) {
        User value = new User(); value.setId(id); value.setUsername(username); value.setFullName(fullName); value.setActive(true);
        return value;
    }
}
