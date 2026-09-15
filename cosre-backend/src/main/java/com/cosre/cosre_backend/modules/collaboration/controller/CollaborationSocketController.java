package com.cosre.cosre_backend.modules.collaboration.controller;

import com.cosre.cosre_backend.modules.collaboration.dto.CollaborationOperation;
import com.cosre.cosre_backend.modules.collaboration.service.CollaborationService;
import jakarta.validation.Valid;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import java.security.Principal;
import java.util.Map;

@Controller
public class CollaborationSocketController {
    private final CollaborationService service;
    private final SimpMessagingTemplate messaging;
    public CollaborationSocketController(CollaborationService service, SimpMessagingTemplate messaging) {
        this.service = service; this.messaging = messaging;
    }
    @MessageMapping("/collaboration/teams/{teamId}/{kind}/operations")
    public void apply(@DestinationVariable long teamId, @DestinationVariable String kind,
            @Valid CollaborationOperation operation, Principal principal) {
        var result = service.apply(teamId, kind, principal.getName(), operation);
        messaging.convertAndSendToUser(principal.getName(), "/queue/collaboration",
                Map.of("operationId", operation.operationId(), "revision", result.revision(), "saved", true));
    }
    @MessageExceptionHandler
    public void error(Exception exception, Principal principal) {
        if (principal != null) messaging.convertAndSendToUser(principal.getName(), "/queue/collaboration",
                Map.of("saved", false, "message", "Không lưu được thao tác. Hãy đồng bộ và thử lại qua API."));
    }
}
