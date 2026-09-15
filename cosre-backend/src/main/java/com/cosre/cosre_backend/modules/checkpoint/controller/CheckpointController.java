package com.cosre.cosre_backend.modules.checkpoint.controller;

import com.cosre.cosre_backend.modules.checkpoint.dto.*;
import com.cosre.cosre_backend.modules.checkpoint.service.CheckpointService;
import jakarta.validation.Valid;
import com.cosre.cosre_backend.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/checkpoints")
@RequiredArgsConstructor
public class CheckpointController {

    private final CheckpointService checkpointService;

    // Tạo Checkpoint mới
    @PostMapping
    public ResponseEntity<ApiResponse<CheckpointResponse>> createCheckpoint(
            @Valid @RequestBody CreateCheckpointRequest request
    ) {
        CheckpointResponse response = checkpointService.createCheckpoint(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(true, "Checkpoint created", response));
    }

    // Cập nhật thông tin Checkpoint
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CheckpointResponse>> updateCheckpoint(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCheckpointRequest request
    ) {
        CheckpointResponse response = checkpointService.updateCheckpoint(id, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Success", response));
    }

    // Lấy thông tin chi tiết một Checkpoint
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CheckpointResponse>> getCheckpointById(@PathVariable Long id) {
        CheckpointResponse response = checkpointService.getCheckpointById(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Success", response));
    }

    // Lấy danh sách Checkpoint của một nhóm
    @GetMapping("/team/{teamId}")
    public ResponseEntity<ApiResponse<List<CheckpointResponse>>> getCheckpointsByTeam(@PathVariable Long teamId) {
        List<CheckpointResponse> response = checkpointService.getCheckpointsByTeam(teamId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Success", response));
    }

    // Sinh viên nộp bài Checkpoint
    @PostMapping("/{id}/submit")
    public ResponseEntity<ApiResponse<CheckpointResponse>> submitCheckpoint(
            @PathVariable Long id,
            @Valid @RequestBody SubmitCheckpointRequest request
    ) {
        CheckpointResponse response = checkpointService.submitCheckpoint(id, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Success", response));
    }

    // Giảng viên duyệt, chấm điểm và feedback Checkpoint
    @PostMapping("/{id}/review")
    public ResponseEntity<ApiResponse<CheckpointResponse>> reviewCheckpoint(
            @PathVariable Long id,
            @Valid @RequestBody ReviewCheckpointRequest request
    ) {
        CheckpointResponse response = checkpointService.reviewCheckpoint(id, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Success", response));
    }
}