package com.cosre.cosre_backend.modules.team.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AssignProjectRequest(@NotNull @Positive Long projectId) { }
