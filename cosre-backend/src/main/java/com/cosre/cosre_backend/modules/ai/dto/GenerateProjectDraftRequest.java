package com.cosre.cosre_backend.modules.ai.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record GenerateProjectDraftRequest(@NotNull @Positive Long syllabusId,
                                          @Size(max = 200) String topic) {}
