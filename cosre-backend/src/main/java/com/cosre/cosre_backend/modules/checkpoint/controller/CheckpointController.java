package com.cosre.cosre_backend.modules.checkpoint.controller;

import com.cosre.cosre_backend.modules.checkpoint.dto.*;
import com.cosre.cosre_backend.modules.checkpoint.service.CheckpointService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/checkpoints")
@RequiredArgsConstructor
public class CheckpointController {

    private final CheckpointService checkpointService;

    // Tạo Checkpoint mới
    @PostMapping
    public ResponseEntity<CheckpointResponse> createCheckpoint(
            @Valid @RequestBody CreateCheckpointRequest request,
            @RequestAttribute("userId") Long currentUserId // Lấy ID người dùng từ Auth Interceptor/Filter
    ) {
        CheckpointResponse response = checkpointService.createCheckpoint(request, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Cập nhật thông tin Checkpoint
    @PutMapping("/{id}")
    public ResponseEntity<CheckpointResponse> updateCheckpoint(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCheckpointRequest request
    ) {
        CheckpointResponse response = checkpointService.updateCheckpoint(id, request);
        return ResponseEntity.ok(response);
    }

    // Lấy thông tin chi tiết một Checkpoint
    @GetMapping("/{id}")
    public ResponseEntity<CheckpointResponse> getCheckpointById(@PathVariable Long id) {
        CheckpointResponse response = checkpointService.getCheckpointById(id);
        return ResponseEntity.ok(response);
    }

    // Lấy danh sách Checkpoint của một nhóm
    @GetMapping("/team/{teamId}")
    public ResponseEntity<List<CheckpointResponse>> getCheckpointsByTeam(@PathVariable Long teamId) {
        List<CheckpointResponse> response = checkpointService.getCheckpointsByTeam(teamId);
        return ResponseEntity.ok(response);
    }

    // Sinh viên nộp bài Checkpoint
    @PostMapping("/{id}/submit")
    public ResponseEntity<CheckpointResponse> submitCheckpoint(
            @PathVariable Long id,
            @Valid @RequestBody SubmitCheckpointRequest request,
            @RequestAttribute("userId") Long currentUserId
    ) {
        CheckpointResponse response = checkpointService.submitCheckpoint(id, request, currentUserId);
        return ResponseEntity.ok(response);
    }

    // Giảng viên duyệt, chấm điểm và feedback Checkpoint
    @PostMapping("/{id}/review")
    public ResponseEntity<CheckpointResponse> reviewCheckpoint(
            @PathVariable Long id,
            @Valid @RequestBody ReviewCheckpointRequest request,
            @RequestAttribute("userId") Long currentUserId
    ) {
        CheckpointResponse response = checkpointService.reviewCheckpoint(id, request, currentUserId);
        return ResponseEntity.ok(response);
    }
}