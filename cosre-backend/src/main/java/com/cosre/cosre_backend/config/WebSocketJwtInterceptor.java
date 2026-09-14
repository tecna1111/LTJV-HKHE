package com.cosre.cosre_backend.config;

import com.cosre.cosre_backend.modules.account.service.AccountService;
import com.cosre.cosre_backend.modules.chat.entity.ChatRoomType;
import com.cosre.cosre_backend.modules.chat.service.ChatRoomAccessService;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class WebSocketJwtInterceptor implements ChannelInterceptor {
    private final JwtConfig jwtConfig;
    private final AccountService accountService;
    private final ChatRoomAccessService chatRoomAccessService;

    public WebSocketJwtInterceptor(JwtConfig jwtConfig, AccountService accountService,
            ChatRoomAccessService chatRoomAccessService) {
        this.jwtConfig = jwtConfig;
        this.accountService = accountService;
        this.chatRoomAccessService = chatRoomAccessService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        // Work with the accessor attached to the original message. Using wrap(message)
        // creates a detached accessor, so the Principal assigned on CONNECT is not
        // retained for later SUBSCRIBE and SEND frames.
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) return message;
        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            authorizeSubscription(accessor);
            return message;
        }
        if (!StompCommand.CONNECT.equals(accessor.getCommand())) return message;

        String header = accessor.getFirstNativeHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new AccessDeniedException("Missing WebSocket authorization token");
        }

        String token = header.substring(7);
        if (!jwtConfig.validateToken(token)) throw new AccessDeniedException("Invalid WebSocket authorization token");

        String username = jwtConfig.getUsername(token);
        var user = accountService.findByUsername(username)
                .filter(value -> value.isActive())
                .orElseThrow(() -> new AccessDeniedException("User is inactive or does not exist"));

        accessor.setUser(new UsernamePasswordAuthenticationToken(
                user.getUsername(), null,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))));
        return message;
    }

    private void authorizeSubscription(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (destination == null || destination.equals("/user/queue/notifications")) return;
        String username = accessor.getUser() == null ? null : accessor.getUser().getName();
        if (username == null) throw new AccessDeniedException("Unauthenticated WebSocket subscription");
        String teamPrefix = "/topic/chat/teams/";
        String classroomPrefix = "/topic/chat/classrooms/";
        try {
            if (destination.startsWith(teamPrefix)) {
                chatRoomAccessService.requireAccess(username, ChatRoomType.TEAM,
                        Long.valueOf(destination.substring(teamPrefix.length())));
            } else if (destination.startsWith(classroomPrefix)) {
                chatRoomAccessService.requireAccess(username, ChatRoomType.CLASSROOM,
                        Long.valueOf(destination.substring(classroomPrefix.length())));
            } else {
                throw new AccessDeniedException("WebSocket destination is not allowed");
            }
        } catch (NumberFormatException exception) {
            throw new AccessDeniedException("Invalid WebSocket destination");
        }
    }
}
