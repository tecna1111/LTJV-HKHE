package com.cosre.cosre_backend.modules.ai.dto;

import java.util.List;

public record GenerateProjectDraftResponse(String title, String description, List<String> objectives,
                                           List<GenerateMilestonesResponse.GeneratedMilestone> milestones) {}
