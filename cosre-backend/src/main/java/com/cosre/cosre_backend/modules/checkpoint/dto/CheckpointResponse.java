package com.cosre.cosre_backend.modules.checkpoint.dto;

import com.cosre.cosre_backend.modules.checkpoint.entity.CheckpointStatus;

import java.time.LocalDateTime;
import java.util.List;

public record CheckpointResponse(
        Long id,
        Long teamId,
        Long milestoneId,
        String title,
        String description,
        LocalDateTime dueAt,
        CheckpointStatus status,
        Long createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime completedAt,
        List<Long> assigneeIds
) {
}