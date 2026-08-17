package com.cosre.cosre_backend.modules.chat.controller;

import com.cosre.cosre_backend.common.dto.ApiResponse;
import com.cosre.cosre_backend.modules.chat.dto.ChatMessageResponse;
import com.cosre.cosre_backend.modules.chat.entity.ChatRoomType;
import com.cosre.cosre_backend.modules.chat.service.ChatService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/chat")
public class ChatRestController {
    private final ChatService service;
    public ChatRestController(ChatService service) { this.service = service; }

    @GetMapping("/teams/{teamId}/messages")
    @PreAuthorize("hasAnyRole('LECTURER','STUDENT')")
    public ApiResponse<List<ChatMessageResponse>> teamHistory(@PathVariable Long teamId, Authentication auth) {
        return new ApiResponse<>(true, "Messages loaded", service.history(ChatRoomType.TEAM, teamId, auth.getName()));
    }

    @GetMapping("/classrooms/{classroomId}/messages")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<ChatMessageResponse>> classroomHistory(@PathVariable Long classroomId, Authentication auth) {
        return new ApiResponse<>(true, "Messages loaded", service.history(ChatRoomType.CLASSROOM, classroomId, auth.getName()));
    }
}
