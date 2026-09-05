package com.cosre.cosre_backend.modules.syllabus.dto;

import jakarta.validation.constraints.*;

public record SyllabusRequest(
        @NotNull @Positive Long subjectId,
        @NotBlank @Size(max = 255) String title,
        @Size(max = 5000) String content,
        @Size(max = 4000) String objectives,
        @NotBlank @Size(max = 30) String version) {}
