package com.cosre.cosre_backend.modules.ai.dto;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
public record GenerateMilestonesRequest(
        @NotNull
        @Positive
        Long syllabusId
) {
}