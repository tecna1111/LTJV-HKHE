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

    private final com.cosre.cosre_backend.modules.collaboration.service.CollaborationService service;

    public WhiteboardController(com.cosre.cosre_backend.modules.collaboration.service.CollaborationService service) {
        this.service = service;
    }
    @GetMapping
    @PreAuthorize("hasAnyRole('LECTURER','STUDENT')")
    public ApiResponse<WhiteboardResponse> get(@PathVariable Long teamId, Authentication auth) {
        var snapshot = service.get(teamId, "whiteboard", auth.getName());
        var json = new com.fasterxml.jackson.databind.ObjectMapper();
        var canvas = json.createObjectNode();
        var objects = canvas.putArray("objects");
        json.valueToTree(snapshot.state()).elements().forEachRemaining(item -> { if (!item.path("deleted").asBoolean()) objects.add(item.path("value")); });
        return ok("Whiteboard loaded", new WhiteboardResponse(null, teamId, canvas.toString(), snapshot.revision(), null, null));
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('LECTURER','STUDENT')")
    public ApiResponse<WhiteboardResponse> save(@PathVariable Long teamId,
            @Valid @RequestBody SaveWhiteboardRequest request, Authentication auth) {
        service.get(teamId, "whiteboard", auth.getName());
        throw new com.cosre.cosre_backend.common.exception.DuplicateResourceException(
                "Bảng vẽ đã dùng đồng bộ thao tác. Hãy tải lại giao diện mới; API lưu toàn bộ bảng đã ngừng để tránh mất nội dung.");
    }
    private <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(true, message, data);
    }
}
