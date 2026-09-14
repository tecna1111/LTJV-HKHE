package com.cosre.cosre_backend.modules.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record MilestoneRequest(

        @NotBlank(message = "Milestone title is required")
        @Size(max = 200, message = "Milestone title must not exceed 200 characters")
        String title,

        @Size(max = 1000, message = "Milestone description must not exceed 1000 characters")
        String description,

        @NotNull(message = "Due offset days is required")
        @PositiveOrZero(message = "Due offset days cannot be negative")
        Integer dueOffsetDays

) {
}
