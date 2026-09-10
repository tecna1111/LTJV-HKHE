package com.cosre.cosre_backend.modules.checkpoint.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.Set;

public record UpdateCheckpointRequest(
        @NotBlank
        @Size(max = 200)
        String title,

        @Size(max = 2000)
        String description,

        @NotNull
        LocalDateTime dueAt,

        Set<Long> assigneeIds
) {
}