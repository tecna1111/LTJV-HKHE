package com.cosre.cosre_backend.modules.project.dto;

import com.cosre.cosre_backend.modules.project.entity.Project;
import com.cosre.cosre_backend.modules.project.entity.ProjectMilestone;
import com.cosre.cosre_backend.modules.project.entity.ProjectStatus;

import java.time.LocalDateTime;
import java.util.List;

public record ProjectResponse(
        Long id,
        String title,
        String description,
        Long subjectId,
        Long createdBy,
        ProjectStatus status,
        List<String> objectives,
        List<MilestoneResponse> milestones,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public record MilestoneResponse(
            Long id,
            String title,
            String description,
            Integer dueOffsetDays,
            Integer displayOrder
    ) {
        public static MilestoneResponse from(ProjectMilestone milestone) {
            return new MilestoneResponse(
                    milestone.getId(),
                    milestone.getTitle(),
                    milestone.getDescription(),
                    milestone.getDueOffsetDays(),
                    milestone.getDisplayOrder()
            );
        }
    }

    public static ProjectResponse from(Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getTitle(),
                project.getDescription(),
                project.getSubjectId(),
                project.getCreatedBy(),
                project.getStatus(),
                List.copyOf(project.getObjectives()),
                project.getMilestones()
                        .stream()
                        .map(MilestoneResponse::from)
                        .toList(),
                project.getCreatedAt(),
                project.getUpdatedAt()
        );
    }
}
