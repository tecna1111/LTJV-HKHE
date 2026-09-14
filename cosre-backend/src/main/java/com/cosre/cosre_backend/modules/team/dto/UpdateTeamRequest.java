package com.cosre.cosre_backend.modules.team.dto;

import jakarta.validation.constraints.*;

public record UpdateTeamRequest(@NotBlank @Size(max = 100) String name, @Size(max = 500) String description) { }
