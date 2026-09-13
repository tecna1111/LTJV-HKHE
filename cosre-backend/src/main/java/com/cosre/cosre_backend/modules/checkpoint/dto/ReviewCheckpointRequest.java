package com.cosre.cosre_backend.modules.checkpoint.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ReviewCheckpointRequest(
        @NotNull Boolean approved,

        @DecimalMin("0.0")
        @DecimalMax("100.0")
        BigDecimal score,

        @Size(max = 2000)
        String feedback
) {
}