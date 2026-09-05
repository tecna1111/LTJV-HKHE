package com.cosre.cosre_backend.modules.chat.dto;

import com.cosre.cosre_backend.modules.account.entity.User;
import com.cosre.cosre_backend.modules.chat.entity.ChatMessage;
import java.time.LocalDateTime;

public record ChatMessageResponse(Long id, String roomType, Long roomId, Long senderId,
        String senderUsername, String senderName, String content, LocalDateTime createdAt) {
    public static ChatMessageResponse from(ChatMessage message, User sender) {
        return new ChatMessageResponse(message.getId(), message.getRoomType(), message.getRoomId(),
                message.getSenderId(), sender.getUsername(), sender.getFullName(), message.getContent(), message.getCreatedAt());
    }
}
