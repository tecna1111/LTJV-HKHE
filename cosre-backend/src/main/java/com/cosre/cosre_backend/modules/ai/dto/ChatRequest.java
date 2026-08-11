package com.cosre.cosre_backend.modules.ai.dto;
import jakarta.validation.constraints.NotBlank;
public record ChatRequest(
    @NotBlank(message = "Prompt is required")
        String prompt
) {

}
