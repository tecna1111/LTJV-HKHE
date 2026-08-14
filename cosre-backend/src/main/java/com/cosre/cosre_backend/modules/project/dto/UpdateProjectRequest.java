package com.cosre.cosre_backend.modules.project.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

public record UpdateProjectRequest(

        @NotBlank(message = "Project title is required")
        @Size(max = 200)
        String title,

        @Size(max = 2000)
        String description,

        @NotNull
        @Positive
        Long subjectId,

        @NotEmpty
        List<
                @NotBlank
                @Size(max = 500)
                String
        > objectives,

        @NotEmpty
        List<@Valid MilestoneRequest> milestones

) {
}
