package com.cosre.cosre_backend.modules.collaboration.controller;

import com.cosre.cosre_backend.common.dto.ApiResponse;
import com.cosre.cosre_backend.modules.collaboration.dto.CollaborationOperation;
import com.cosre.cosre_backend.modules.collaboration.service.CollaborationService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/collaboration/teams/{teamId}/documents/{kind}")
@PreAuthorize("hasAnyRole('LECTURER','STUDENT')")
public class CollaborationController {
    private final CollaborationService service;
    public CollaborationController(CollaborationService service) { this.service = service; }
    @GetMapping
    public ApiResponse<CollaborationService.Snapshot> get(@PathVariable long teamId, @PathVariable String kind, Authentication auth) {
        return new ApiResponse<>(true, "Đã đồng bộ", service.get(teamId, kind, auth.getName()));
    }
    @PostMapping("/operations")
    public ApiResponse<CollaborationService.Snapshot> apply(@PathVariable long teamId, @PathVariable String kind,
            @Valid @RequestBody CollaborationOperation operation, Authentication auth) {
        return new ApiResponse<>(true, "Đã lưu", service.apply(teamId, kind, auth.getName(), operation));
    }
}
