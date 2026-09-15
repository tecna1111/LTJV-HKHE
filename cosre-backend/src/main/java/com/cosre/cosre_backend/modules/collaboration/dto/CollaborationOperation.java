package com.cosre.cosre_backend.modules.collaboration.dto;

import java.util.Map;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CollaborationOperation(
        @Pattern(regexp = "[a-zA-Z0-9_-]{8,80}") @NotNull String operationId,
        @Pattern(regexp = "object.put|object.delete|text.edit") @NotNull String action,
        @NotNull Map<String, Object> payload) {}
