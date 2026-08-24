package com.cosre.cosre_backend.modules.team.dto;
import com.cosre.cosre_backend.modules.project.dto.ProjectResponse; import java.util.Set;
public record TeamWorkspaceResponse(TeamResponse team, ProjectResponse project, Set<Long> completedMilestoneIds, int progressPercent) {}
