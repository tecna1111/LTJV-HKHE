package com.cosre.cosre_backend.modules.ai.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
public record ChatRequest(
        @NotBlank(message = "Prompt is required")
        @Size(max = 4000)
        String prompt,

        @Positive
        Long teamId
) {
}