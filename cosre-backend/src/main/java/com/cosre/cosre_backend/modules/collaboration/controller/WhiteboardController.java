package com.cosre.cosre_backend.modules.collaboration.controller;
import com.cosre.cosre_backend.common.dto.ApiResponse;
import com.cosre.cosre_backend.modules.collaboration.dto.SaveWhiteboardRequest;
import com.cosre.cosre_backend.modules.collaboration.dto.WhiteboardResponse;
import com.cosre.cosre_backend.modules.collaboration.service.WhiteboardService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/v1/collaboration/teams/{teamId}/whiteboard")
public class WhiteboardController {

    private final WhiteboardService service;

    public WhiteboardController(WhiteboardService service) {
        this.service = service;
    }
    @GetMapping
    @PreAuthorize("hasAnyRole('LECTURER','STUDENT')")
    public ApiResponse<WhiteboardResponse> get(@PathVariable Long teamId, Authentication auth) {
        return ok("Whiteboard loaded", service.getByTeam(teamId, auth.getName()));
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('LECTURER','STUDENT')")
    public ApiResponse<WhiteboardResponse> save(@PathVariable Long teamId,
            @Valid @RequestBody SaveWhiteboardRequest request, Authentication auth) {
        return ok("Whiteboard saved", service.save(teamId, auth.getName(), request));
    }
    private <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(true, message, data);
    }
}