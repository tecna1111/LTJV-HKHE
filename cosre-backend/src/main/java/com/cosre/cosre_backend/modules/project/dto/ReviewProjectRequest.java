package com.cosre.cosre_backend.modules.project.dto;

import jakarta.validation.constraints.Size;

public record ReviewProjectRequest(boolean approved, @Size(max = 1000) String note) {}
