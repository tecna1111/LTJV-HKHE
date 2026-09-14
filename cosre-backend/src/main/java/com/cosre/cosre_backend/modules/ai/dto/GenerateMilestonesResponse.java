package com.cosre.cosre_backend.modules.ai.dto;
import java.util.List;
public record GenerateMilestonesResponse(
        List<GeneratedMilestone> milestones
) {
    public record GeneratedMilestone(
            String title,
            String description,
            Integer dueOffsetDays
    ) {
    }
}