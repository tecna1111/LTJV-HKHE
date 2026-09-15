package com.cosre.cosre_backend.modules.team.service;

import com.cosre.cosre_backend.common.constants.RoleEnum;
import com.cosre.cosre_backend.common.exception.BusinessRuleException;
import com.cosre.cosre_backend.common.exception.DuplicateResourceException;
import com.cosre.cosre_backend.common.exception.ResourceNotFoundException;
import com.cosre.cosre_backend.modules.account.entity.User;
import com.cosre.cosre_backend.modules.account.repository.UserRepository;
import com.cosre.cosre_backend.modules.team.dto.CreateTeamRequest;
import com.cosre.cosre_backend.modules.team.dto.UpdateTeamRequest;
import com.cosre.cosre_backend.modules.team.entity.Team;
import com.cosre.cosre_backend.modules.team.repository.TeamRepository;
import com.cosre.cosre_backend.modules.team.repository.TeamMilestoneProgressRepository;
import com.cosre.cosre_backend.modules.classroom.repository.ClassroomRepository;
import com.cosre.cosre_backend.modules.project.repository.ProjectRepository;
import com.cosre.cosre_backend.modules.project.repository.ClassroomProjectRepository;
import com.cosre.cosre_backend.modules.project.entity.ProjectStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@Transactional
public class TeamService {
    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final ClassroomRepository classroomRepository;
    private final ProjectRepository projectRepository;
    private final ClassroomProjectRepository classroomProjectRepository;
    private final TeamMilestoneProgressRepository progressRepository;
    private final com.cosre.cosre_backend.modules.kanban.service.KanbanService kanbanService;

    public TeamService(TeamRepository teamRepository, UserRepository userRepository, ClassroomRepository classroomRepository,
            ProjectRepository projectRepository, ClassroomProjectRepository classroomProjectRepository, TeamMilestoneProgressRepository progressRepository,
            com.cosre.cosre_backend.modules.kanban.service.KanbanService kanbanService) {
        this.teamRepository = teamRepository;
        this.userRepository = userRepository;
        this.classroomRepository = classroomRepository;
        this.projectRepository = projectRepository;
        this.classroomProjectRepository = classroomProjectRepository;
        this.progressRepository = progressRepository;
        this.kanbanService = kanbanService;
    }

    public Team create(CreateTeamRequest request, String username) {
        if (teamRepository.existsByClassroomIdAndNameIgnoreCase(request.classroomId(), request.name().trim()))
            throw new DuplicateResourceException("Team name already exists in this classroom");
        User lecturer = requireUser(username);
        requireManagedClassroom(request.classroomId(), lecturer);
        Team team = new Team();
        team.setName(request.name().trim());
        team.setDescription(clean(request.description()));
        team.setClassroomId(request.classroomId());
        if (request.projectId() != null) validateProject(request.classroomId(), request.projectId());
        team.setProjectId(request.projectId());
        team.setLecturer(lecturer);
        Set<Long> ids = request.memberIds() == null ? new LinkedHashSet<>() : new LinkedHashSet<>(request.memberIds());
        if (request.leaderId() != null) ids.add(request.leaderId());
        for (Long id : ids) team.getMembers().add(requireAvailableStudent(id, request.classroomId()));
        if (request.leaderId() != null) team.setLeader(findMember(team, request.leaderId()));
        return teamRepository.save(team);
    }

    @Transactional(readOnly = true)
    public List<Team> listMine(String username) {
        requireUser(username);
        return teamRepository.findDistinctByLecturerUsernameOrMembersUsernameOrderByUpdatedAtDesc(username, username).stream()
            .filter(team -> {
                try { requireAccessible(team.getId(), username); return true; }
                catch (org.springframework.security.access.AccessDeniedException denied) { return false; }
            }).toList();
    }

    @Transactional(readOnly = true)
    public List<Team> listByClassroom(Long classroomId, String username) {
        User actor = requireUser(username);
        if (actor.getRole() == RoleEnum.LECTURER) requireManagedClassroom(classroomId, actor);
        else if (actor.getRole() != RoleEnum.STAFF && actor.getRole() != RoleEnum.HEAD_DEPT)
            throw new org.springframework.security.access.AccessDeniedException("You cannot list teams in this class");
        return teamRepository.findByClassroomIdOrderByUpdatedAtDesc(classroomId);
    }

    @Transactional(readOnly = true)
    public Team get(Long id, String username) { return requireAccessible(id, username); }

    public Team update(Long id, UpdateTeamRequest request, String username) {
        Team team = requireAccessible(id, username);
        User actor = requireUser(username);
        if (!team.getLecturer().getId().equals(actor.getId())
                && (team.getLeader() == null || !team.getLeader().getId().equals(actor.getId())))
            throw new org.springframework.security.access.AccessDeniedException("Only lecturer or leader can edit team information");
        String name = request.name().trim();
        if (!team.getName().equalsIgnoreCase(name) && teamRepository.existsByClassroomIdAndNameIgnoreCase(team.getClassroomId(), name))
            throw new DuplicateResourceException("Team name already exists in this classroom");
        team.setName(name);
        team.setDescription(clean(request.description()));
        return team;
    }

    public Team addMember(Long id, Long studentId, boolean leader, String username) {
        Team team = requireOwned(id, username);
        User student = team.getMembers().stream().filter(user -> user.getId().equals(studentId)).findFirst()
                .orElseGet(() -> requireAvailableStudent(studentId, team.getClassroomId()));
        team.getMembers().add(student);
        if (leader) team.setLeader(student);
        return team;
    }

    public Team removeMember(Long id, Long studentId, String username) {
        Team team = requireOwned(id, username);
        long assigned = entityManager.createQuery("select count(t) from KanbanTask t, KanbanBoard b where t.boardId=b.id and b.teamId=:team and :student member of t.assigneeIds", Long.class)
                .setParameter("team", id).setParameter("student", studentId).getSingleResult();
        if (assigned > 0) throw new BusinessRuleException("Reassign this member's Kanban tasks before removing them");
        User student = findMember(team, studentId);
        if (team.getLeader() != null && team.getLeader().getId().equals(studentId)) team.setLeader(null);
        team.getMembers().remove(student);
        return team;
    }

    public Team assignProject(Long id, Long projectId, String username) {
        Team team = requireOwned(id, username);
        if (!Objects.equals(team.getProjectId(), projectId)) {
            long linked = entityManager.createQuery("select count(t) from KanbanTask t, KanbanBoard b where t.boardId=b.id and b.teamId=:team and (t.milestoneId is not null or t.checkpointId is not null)", Long.class)
                    .setParameter("team", id).getSingleResult();
            if (linked > 0) throw new BusinessRuleException("Remove Kanban milestone/checkpoint links before changing project");
        }
        validateProject(team.getClassroomId(), projectId);
        team.setProjectId(projectId);
        return team;
    }

    public void delete(Long id, String username) {
        Team team = requireOwned(id, username);
        kanbanService.deleteBoard(id, username);
        teamRepository.delete(team);
    }

    @Transactional(readOnly = true)
    public com.cosre.cosre_backend.modules.team.dto.TeamWorkspaceResponse workspace(Long id, String username) {
        Team team = requireAccessible(id, username);
        var project = team.getProjectId() == null ? null : projectRepository.findById(team.getProjectId()).orElse(null);
        Set<Long> completed = progressRepository.findByTeamId(id).stream().map(item -> item.getMilestoneId()).collect(java.util.stream.Collectors.toSet());
        int total = project == null ? 0 : project.getMilestones().size();
        int percent = total == 0 ? 0 : completed.size() * 100 / total;
        return new com.cosre.cosre_backend.modules.team.dto.TeamWorkspaceResponse(com.cosre.cosre_backend.modules.team.dto.TeamResponse.from(team),
                project == null ? null : com.cosre.cosre_backend.modules.project.dto.ProjectResponse.from(project), completed, percent);
    }

    public com.cosre.cosre_backend.modules.team.dto.TeamWorkspaceResponse setMilestoneDone(Long teamId, Long milestoneId, boolean done, String username) {
        Team team = requireAccessible(teamId, username); User actor = requireUser(username);
        if (team.getLeader() == null || !team.getLeader().getId().equals(actor.getId())) throw new BusinessRuleException("Only the team leader can update milestones");
        var project = team.getProjectId() == null ? null : projectRepository.findById(team.getProjectId()).orElse(null);
        if (project == null || project.getMilestones().stream().noneMatch(item -> item.getId().equals(milestoneId))) throw new ResourceNotFoundException("Milestone not found in team project");
        var key = new com.cosre.cosre_backend.modules.team.entity.TeamMilestoneProgressId(teamId, milestoneId);
        if (done && !progressRepository.existsById(key)) { var item = new com.cosre.cosre_backend.modules.team.entity.TeamMilestoneProgress(); item.setTeamId(teamId); item.setMilestoneId(milestoneId); item.setCompletedBy(actor.getId()); progressRepository.save(item); }
        if (!done) progressRepository.deleteById(key);
        return workspace(teamId, username);
    }

    @Transactional(readOnly = true)
    public List<User> availableStudents(Long classroomId, String username) {
        User lecturer = requireUser(username);
        var classroom = classroomRepository.findDetailedById(classroomId)
                .orElseThrow(() -> new ResourceNotFoundException("Classroom not found"));
        if (classroom.getLecturers().stream().noneMatch(user -> user.getId().equals(lecturer.getId())))
            throw new BusinessRuleException("Lecturer is not assigned to this classroom");
        return classroom.getStudents().stream()
                .filter(user -> user.isActive() && user.getRole() == RoleEnum.STUDENT)
                .filter(user -> !teamRepository.existsByClassroomIdAndMembersId(classroomId, user.getId()))
                .sorted(Comparator.comparing(User::getFullName,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();
    }

    private Team requireOwned(Long id, String username) {
        Team team = requireTeam(id);
        User actor = requireUser(username);
        if (actor.getRole() != RoleEnum.LECTURER || !team.getLecturer().getUsername().equals(username))
            throw new org.springframework.security.access.AccessDeniedException("You cannot manage this team");
        requireManagedClassroom(team.getClassroomId(), actor);
        return team;
    }
    private Team requireAccessible(Long id, String username) {
        Team team = requireTeam(id);
        User actor = requireUser(username);
        var classroom = classroomRepository.findDetailedById(team.getClassroomId()).orElseThrow(() -> new ResourceNotFoundException("Classroom not found"));
        boolean member = actor.getRole() == RoleEnum.STUDENT && team.getMembers().stream().anyMatch(user -> user.getUsername().equals(username))
            && classroom.getStudents().stream().anyMatch(user -> user.getId().equals(actor.getId()));
        boolean lecturer = actor.getRole() == RoleEnum.LECTURER && team.getLecturer().getId().equals(actor.getId())
            && classroom.getLecturers().stream().anyMatch(user -> user.getId().equals(actor.getId()));
        if (!lecturer && !member) throw new org.springframework.security.access.AccessDeniedException("You cannot view this team");
        return team;
    }
    private Team requireTeam(Long id) {
        Team team = org.springframework.transaction.support.TransactionSynchronizationManager.isCurrentTransactionReadOnly()
            ? entityManager.find(Team.class, id)
            : entityManager.find(Team.class, id, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        if (team == null) throw new ResourceNotFoundException("Team not found");
        org.hibernate.Hibernate.initialize(team.getLecturer());
        org.hibernate.Hibernate.initialize(team.getLeader());
        org.hibernate.Hibernate.initialize(team.getMembers());
        return team;
    }
    private User requireUser(String username) { return userRepository.findByUsername(username).filter(User::isActive).orElseThrow(() -> new org.springframework.security.access.AccessDeniedException("Active account required")); }
    private User requireAvailableStudent(Long id, Long classroomId) {
        User user = userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Student not found"));
        if (user.getRole() != RoleEnum.STUDENT || !user.isActive()) throw new BusinessRuleException("Only active students can join a team");
        var classroom = classroomRepository.findDetailedById(classroomId).orElseThrow(() -> new ResourceNotFoundException("Classroom not found"));
        if (classroom.getStudents().stream().noneMatch(student -> student.getId().equals(id))) throw new BusinessRuleException("Student is not assigned to this classroom");
        if (teamRepository.existsByClassroomIdAndMembersId(classroomId, id)) throw new BusinessRuleException("Student already belongs to a team in this classroom");
        return user;
    }
    private void requireManagedClassroom(Long classroomId, User lecturer) {
        var classroom = classroomRepository.findDetailedById(classroomId).orElseThrow(() -> new ResourceNotFoundException("Classroom not found"));
        if (lecturer.getRole() != RoleEnum.LECTURER || classroom.getLecturers().stream().noneMatch(user -> user.getId().equals(lecturer.getId()))) throw new org.springframework.security.access.AccessDeniedException("Lecturer is not assigned to this classroom");
    }
    private void validateProject(Long classroomId, Long projectId) {
        var project = projectRepository.findById(projectId).orElseThrow(() -> new ResourceNotFoundException("Project not found"));
        if (project.getStatus() != ProjectStatus.APPROVED || !classroomProjectRepository.existsByClassroomIdAndProjectId(classroomId, projectId))
            throw new BusinessRuleException("Project must be approved and assigned to this classroom");
    }
    private User findMember(Team team, Long studentId) { return team.getMembers().stream().filter(user -> user.getId().equals(studentId)).findFirst().orElseThrow(() -> new ResourceNotFoundException("Team member not found")); }
    private String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
