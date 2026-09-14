package com.cosre.cosre_backend.modules.checkpoint.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.Set;

public record CreateCheckpointRequest(
        @NotNull Long teamId,
        Long milestoneId,

        @NotBlank
        @Size(max = 200)
        String title,

        @Size(max = 2000)
        String description,

        @NotNull
        @Future
        LocalDateTime dueAt,

        Set<Long> assigneeIds
) {
}