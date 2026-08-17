package com.cosre.cosre_backend.modules.chat.controller;

import com.cosre.cosre_backend.modules.chat.dto.ChatMessageResponse;
import com.cosre.cosre_backend.modules.chat.dto.SendChatMessageRequest;
import com.cosre.cosre_backend.modules.chat.entity.ChatRoomType;
import com.cosre.cosre_backend.modules.chat.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import java.security.Principal;

@Controller
public class ChatWebSocketController {
    private final SimpMessagingTemplate messagingTemplate;
    private final ChatService chatService;

    public ChatWebSocketController(SimpMessagingTemplate messagingTemplate, ChatService chatService) {
        this.messagingTemplate = messagingTemplate;
        this.chatService = chatService;
    }

    @MessageMapping("/chat/teams/{teamId}/send")
    public void sendTeam(@DestinationVariable Long teamId, @Valid SendChatMessageRequest request, Principal principal) {
        ChatMessageResponse response = chatService.send(ChatRoomType.TEAM, teamId, principal.getName(), request);
        messagingTemplate.convertAndSend("/topic/chat/teams/" + teamId, response);
    }

    @MessageMapping("/chat/classrooms/{classroomId}/send")
    public void sendClassroom(@DestinationVariable Long classroomId, @Valid SendChatMessageRequest request,
            Principal principal) {
        ChatMessageResponse response = chatService.send(ChatRoomType.CLASSROOM, classroomId, principal.getName(), request);
        messagingTemplate.convertAndSend("/topic/chat/classrooms/" + classroomId, response);
    }
}
