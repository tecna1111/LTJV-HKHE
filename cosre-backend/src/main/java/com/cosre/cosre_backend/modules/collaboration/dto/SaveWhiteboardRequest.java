package com.cosre.cosre_backend.modules.collaboration.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
public record SaveWhiteboardRequest(
        @NotBlank
        @Size(max = 2_000_000)
        String canvasData,

        @NotNull
        @PositiveOrZero
        Long version
) {
}