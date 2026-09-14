package com.cosre.cosre_backend.modules.checkpoint.service;

import com.cosre.cosre_backend.common.constants.RoleEnum;
import com.cosre.cosre_backend.common.exception.BusinessRuleException;
import com.cosre.cosre_backend.common.exception.ResourceNotFoundException;
import com.cosre.cosre_backend.modules.account.entity.User;
import com.cosre.cosre_backend.modules.account.repository.UserRepository;
import com.cosre.cosre_backend.modules.checkpoint.entity.Checkpoint;
import com.cosre.cosre_backend.modules.checkpoint.repository.CheckpointAssignmentRepository;
import com.cosre.cosre_backend.modules.project.repository.ProjectMilestoneRepository;
import com.cosre.cosre_backend.modules.team.entity.Team;
import com.cosre.cosre_backend.modules.team.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CheckpointAccessService {
    private final UserRepository users;
    private final TeamRepository teams;
    private final CheckpointAssignmentRepository assignments;
    private final ProjectMilestoneRepository milestones;

    public User currentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken)
            throw new AccessDeniedException("Authentication required");
        return users.findByUsername(auth.getName()).filter(User::isActive)
                .orElseThrow(() -> new AccessDeniedException("Active account required"));
    }

    public Team view(Long teamId) {
        User user = currentUser();
        Team team = teams.findById(teamId).orElseThrow(() -> new ResourceNotFoundException("Team not found"));
        if (!isMember(team, user.getId()) && !isLecturer(team, user))
            throw new AccessDeniedException("No access to this team");
        return team;
    }

    public Team leader(Long teamId) {
        Team team = view(teamId);
        User user = currentUser();
        if (user.getRole() != RoleEnum.STUDENT || team.getLeader() == null
                || !Objects.equals(team.getLeader().getId(), user.getId()))
            throw new AccessDeniedException("Only the team leader can manage checkpoints");
        return team;
    }

    public void lecturer(Long teamId) {
        if (!isLecturer(view(teamId), currentUser()))
            throw new AccessDeniedException("Only the managing lecturer can review checkpoints");
    }

    public void assignee(Checkpoint checkpoint) {
        Team team = view(checkpoint.getTeamId());
        User user = currentUser();
        if (!isMember(team, user.getId())
                || !assignments.existsByCheckpointIdAndStudentId(checkpoint.getId(), user.getId()))
            throw new AccessDeniedException("Only an assigned team member can submit this checkpoint");
    }

    public void validateAssignees(Team team, Set<Long> ids) {
        if (ids != null && ids.stream().anyMatch(id -> id == null || !isMember(team, id)))
            throw new BusinessRuleException("Assignees must be students in this team");
    }

    public void validateMilestone(Team team, Long milestoneId) {
        if (milestoneId == null) return;
        var milestone = milestones.findById(milestoneId)
                .orElseThrow(() -> new ResourceNotFoundException("Milestone not found"));
        if (team.getProjectId() == null || !Objects.equals(team.getProjectId(), milestone.getProject().getId()))
            throw new BusinessRuleException("Milestone must belong to the team's project");
    }

    private boolean isMember(Team team, Long id) {
        return team.getMembers().stream().anyMatch(u -> Objects.equals(u.getId(), id) && u.getRole() == RoleEnum.STUDENT);
    }

    private boolean isLecturer(Team team, User user) {
        return user.getRole() == RoleEnum.LECTURER && team.getLecturer() != null
                && Objects.equals(team.getLecturer().getId(), user.getId());
    }
}
