package com.cosre.cosre_backend.config;

import com.cosre.cosre_backend.modules.account.service.AccountService;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class WebSocketJwtInterceptor implements ChannelInterceptor {
    private final JwtConfig jwtConfig;
    private final AccountService accountService;

    public WebSocketJwtInterceptor(JwtConfig jwtConfig, AccountService accountService) {
        this.jwtConfig = jwtConfig;
        this.accountService = accountService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
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
}
