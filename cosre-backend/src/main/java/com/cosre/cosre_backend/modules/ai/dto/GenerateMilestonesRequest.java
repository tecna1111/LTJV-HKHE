package com.cosre.cosre_backend.modules.ai.dto;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
public record GenerateMilestonesRequest(
        @NotNull
        @Positive
        Long syllabusId,
        @Size(max = 20) List<@NotBlank @Size(max = 500) String> objectives
) {
}
