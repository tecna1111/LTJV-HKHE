package com.cosre.cosre_backend.modules.project.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

public record CreateProjectRequest(

        @NotBlank(message = "Project title is required")
        @Size(max = 200, message = "Project title must not exceed 200 characters")
        String title,

        @Size(max = 2000, message = "Description must not exceed 2000 characters")
        String description,

        @NotNull(message = "Subject is required")
        @Positive(message = "Subject ID must be positive")
        Long subjectId,

        @NotEmpty(message = "Project must contain at least one objective")
        List<
                @NotBlank(message = "Objective cannot be blank")
                @Size(max = 500, message = "Objective must not exceed 500 characters")
                String
        > objectives,

        @NotEmpty(message = "Project must contain at least one milestone")
        List<@Valid MilestoneRequest> milestones

) {
}
