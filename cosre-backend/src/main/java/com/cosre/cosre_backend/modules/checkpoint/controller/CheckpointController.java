package com.cosre.cosre_backend.modules.checkpoint.controller;

import com.cosre.cosre_backend.modules.checkpoint.dto.*;
import com.cosre.cosre_backend.modules.checkpoint.service.CheckpointService;
import com.cosre.cosre_backend.modules.notification.service.NotificationService;
import jakarta.validation.Valid;
import com.cosre.cosre_backend.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import java.util.List;

@RestController
@RequestMapping("/api/v1/checkpoints")
@RequiredArgsConstructor
public class CheckpointController {

    private final CheckpointService checkpointService;
    private final NotificationService notifications;

    @PostMapping
    public ResponseEntity<ApiResponse<CheckpointResponse>> createCheckpoint(
            @Valid @RequestBody CreateCheckpointRequest request
    ) {
        CheckpointResponse response = checkpointService.createCheckpoint(request);
        notifications.notifyTeamEvent(response.teamId(), authenticationName(), "CHECKPOINT_CREATED:" + response.id(),
                "CHECKPOINT_CREATED", "Checkpoint mới", response.title(), "/teams/" + response.teamId() + "/workspace");
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(true, "Checkpoint created", response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CheckpointResponse>> updateCheckpoint(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCheckpointRequest request
    ) {
        CheckpointResponse response = checkpointService.updateCheckpoint(id, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Success", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CheckpointResponse>> getCheckpointById(@PathVariable Long id) {
        CheckpointResponse response = checkpointService.getCheckpointById(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Success", response));
    }

    @GetMapping("/team/{teamId}")
    public ResponseEntity<ApiResponse<List<CheckpointResponse>>> getCheckpointsByTeam(@PathVariable Long teamId) {
        List<CheckpointResponse> response = checkpointService.getCheckpointsByTeam(teamId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Success", response));
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<ApiResponse<CheckpointResponse>> submitCheckpoint(
            @PathVariable Long id,
            @Valid @RequestBody SubmitCheckpointRequest request
    ) {
        CheckpointResponse response = checkpointService.submitCheckpoint(id, request);
        notifications.notifyTeamEvent(response.teamId(), authenticationName(),
                "CHECKPOINT_SUBMITTED:" + id + ":" + java.util.UUID.randomUUID(),
                "CHECKPOINT_SUBMITTED", "Checkpoint đã được nộp", response.title(),
                "/teams/" + response.teamId() + "/workspace");
        return ResponseEntity.ok(new ApiResponse<>(true, "Success", response));
    }

    @PostMapping("/{id}/review")
    public ResponseEntity<ApiResponse<CheckpointResponse>> reviewCheckpoint(
            @PathVariable Long id,
            @Valid @RequestBody ReviewCheckpointRequest request
    ) {
        CheckpointResponse response = checkpointService.reviewCheckpoint(id, request);
        notifications.notifyTeamEvent(response.teamId(), authenticationName(),
                "CHECKPOINT_REVIEWED:" + id + ":" + java.util.UUID.randomUUID(),
                "CHECKPOINT_REVIEWED", Boolean.TRUE.equals(request.approved()) ? "Checkpoint hoàn thành" : "Checkpoint cần bổ sung",
                response.title(), "/teams/" + response.teamId() + "/workspace");
        return ResponseEntity.ok(new ApiResponse<>(true, "Success", response));
    }

    private String authenticationName() {
        Authentication authentication = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        return authentication == null ? "" : authentication.getName();
    }
}
