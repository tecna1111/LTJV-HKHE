package com.cosre.cosre_backend.modules.team.dto;

import jakarta.validation.constraints.*;
import java.util.Set;

public record CreateTeamRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 500) String description,
        @NotNull @Positive Long classroomId,
        @Positive Long projectId,
        @Positive Long leaderId,
        Set<@Positive Long> memberIds) { }
