package com.cosre.cosre_backend.modules.evaluation.service;

import com.cosre.cosre_backend.common.exception.*;
import com.cosre.cosre_backend.common.constants.RoleEnum;
import com.cosre.cosre_backend.modules.account.entity.User;
import com.cosre.cosre_backend.modules.account.repository.UserRepository;
import com.cosre.cosre_backend.modules.project.entity.Project;
import com.cosre.cosre_backend.modules.project.repository.ProjectRepository;
import com.cosre.cosre_backend.modules.team.entity.Team;
import com.cosre.cosre_backend.modules.team.repository.TeamRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EvaluationAccessService {
    private final TeamRepository teams;
    private final ProjectRepository projects;
    private final UserRepository users;
    private final EntityManager em;

    public User currentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) throw new AccessDeniedException("Authentication required");
        return users.findByUsername(auth.getName()).orElseThrow(() -> new AccessDeniedException("User not found"));
    }

    public Team team(Long teamId, Long projectId) {
        Team team = teams.findById(teamId).orElseThrow(() -> new ResourceNotFoundException("Team not found"));
        if (projectId == null || !Objects.equals(team.getProjectId(), projectId))
            throw new BusinessRuleException("Team does not belong to this project");
        return team;
    }

    public void member(Team team, Long studentId) {
        if (team.getMembers().stream().noneMatch(u -> u.getId().equals(studentId) && u.getRole() == RoleEnum.STUDENT))
            throw new AccessDeniedException("Student is not a member of this team");
    }

    public Team lecturer(Long teamId, Long projectId) {
        Team team = team(teamId, projectId);
        User user = currentUser();
        if (user.getRole() != RoleEnum.LECTURER || !team.getLecturer().getId().equals(user.getId()))
            throw new AccessDeniedException("Only the managing lecturer can evaluate this team");
        return team;
    }

    public void viewProject(Long projectId) {
        User user = currentUser();
        Project project = projects.findById(projectId).orElseThrow(() -> new ResourceNotFoundException("Project not found"));
        if (project.getCreatedBy().equals(user.getId())) return;
        boolean assigned = teams.findDistinctByLecturerUsernameOrMembersUsernameOrderByUpdatedAtDesc(user.getUsername(), user.getUsername())
                .stream().anyMatch(t -> Objects.equals(t.getProjectId(), projectId));
        if (!assigned) throw new AccessDeniedException("No access to this project");
    }

    // Shared lock order for rubric changes, submissions, final grades and closing a round.
    @Transactional
    public void lockProject(Long projectId) {
        if (em.find(Project.class, projectId, LockModeType.PESSIMISTIC_WRITE) == null)
            throw new ResourceNotFoundException("Project not found");
    }
}
