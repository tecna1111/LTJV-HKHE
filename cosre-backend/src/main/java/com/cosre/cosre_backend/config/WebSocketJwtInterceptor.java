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
    private final com.cosre.cosre_backend.modules.collaboration.service.CollaborationAccessService collaborationAccess;

    public WebSocketJwtInterceptor(JwtConfig jwtConfig, AccountService accountService,
            ChatRoomAccessService chatRoomAccessService,
            com.cosre.cosre_backend.modules.collaboration.service.CollaborationAccessService collaborationAccess) {
        this.jwtConfig = jwtConfig;
        this.accountService = accountService;
        this.chatRoomAccessService = chatRoomAccessService;
        this.collaborationAccess = collaborationAccess;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        // Work with the accessor attached to the original message. Using wrap(message)
        // creates a detached accessor, so the Principal assigned on CONNECT is not
        // retained for later SUBSCRIBE and SEND frames.
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) return message;
        if (StompCommand.SEND.equals(accessor.getCommand())) {
            authorizeSend(accessor);
            return message;
        }
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
        if (!jwtConfig.validateToken(token) || !jwtConfig.isAccessToken(token)) throw new AccessDeniedException("Invalid WebSocket authorization token");
        if (accessor.getSessionAttributes() != null) accessor.getSessionAttributes().put("accessToken", token);

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
        String username = accessor.getUser() == null ? null : accessor.getUser().getName();
        if (username == null) throw new AccessDeniedException("Unauthenticated WebSocket subscription");
        requireActive(accessor, username);
        if ("/user/queue/notifications".equals(destination) || "/user/queue/collaboration".equals(destination)) return;
        if (destination == null) throw new AccessDeniedException("Missing destination");
        var collaboration = java.util.regex.Pattern.compile("^/topic/collaboration/teams/([0-9]+)/(whiteboard|text)$").matcher(destination);
        if (collaboration.matches()) {
            collaborationAccess.requireAccess(parseId(collaboration.group(1)), username);
            return;
        }
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

    private long parseId(String id) {
        try { return Long.parseLong(id); }
        catch (NumberFormatException e) { throw new AccessDeniedException("Invalid destination ID"); }
    }
    private void requireActive(StompHeaderAccessor accessor, String username) {
        Object token = accessor.getSessionAttributes() == null ? null : accessor.getSessionAttributes().get("accessToken");
        if (!(token instanceof String value) || !jwtConfig.validateToken(value) || !jwtConfig.isAccessToken(value))
            throw new AccessDeniedException("WebSocket session expired; reconnect with a new access token");
        accountService.findByUsername(username).filter(u -> u.isActive())
                .orElseThrow(() -> new AccessDeniedException("Inactive WebSocket user"));
    }
    private void authorizeSend(StompHeaderAccessor accessor) {
        String username = accessor.getUser() == null ? null : accessor.getUser().getName();
        if (username == null) throw new AccessDeniedException("Unauthenticated WebSocket send");
        requireActive(accessor, username);
        String destination = accessor.getDestination();
        if (destination == null) throw new AccessDeniedException("Missing destination");
        var collaboration = java.util.regex.Pattern.compile("^/app/collaboration/teams/([0-9]+)/(whiteboard|text)/operations$").matcher(destination);
        if (collaboration.matches()) {
            collaborationAccess.requireAccess(parseId(collaboration.group(1)), username);
            return;
        }
        var chat = java.util.regex.Pattern.compile("^/app/chat/(teams|classrooms)/([0-9]+)/send$").matcher(destination);
        if (chat.matches()) {
            chatRoomAccessService.requireAccess(username, chat.group(1).equals("teams") ? ChatRoomType.TEAM : ChatRoomType.CLASSROOM, parseId(chat.group(2)));
            return;
        }
        throw new AccessDeniedException("WebSocket send destination is not allowed");
    }
}
