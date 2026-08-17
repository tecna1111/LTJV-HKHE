package com.cosre.cosre_backend.modules.chat.controller;

import com.cosre.cosre_backend.modules.chat.dto.ChatPayload;
import com.cosre.cosre_backend.modules.chat.entity.ChatMessage;
import com.cosre.cosre_backend.modules.chat.repository.ChatMessageRepository;
import com.cosre.cosre_backend.modules.chat.service.ChatRoomAccessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.LocalDateTime;

@Controller
public class ChatWebSocketController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private ChatRoomAccessService accessService;

    @Autowired
    private ChatMessageRepository chatRepository;

    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload ChatPayload payload, Principal principal) {
        Long userId = Long.parseLong(principal.getName());
        
        if (accessService.canAccess(userId, payload.getRoomType(), payload.getRoomId())) {
            
            ChatMessage chatMessage = ChatMessage.builder()
                    .roomType(payload.getRoomType())
                    .roomId(payload.getRoomId())
                    .senderId(userId)
                    .content(payload.getContent())
                    .build();
            chatRepository.save(chatMessage);

    
            payload.setSenderId(userId);
            payload.setSenderName("Người dùng " + userId);
            payload.setTimestamp(LocalDateTime.now().toString());

            String destination = String.format("/topic/rooms/%s/%d", payload.getRoomType(), payload.getRoomId());
            messagingTemplate.convertAndSend(destination, payload);
        }
    }
}