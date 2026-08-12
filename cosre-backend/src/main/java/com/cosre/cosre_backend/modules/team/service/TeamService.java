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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@Transactional
public class TeamService {
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;

    public TeamService(TeamRepository teamRepository, UserRepository userRepository) {
        this.teamRepository = teamRepository;
        this.userRepository = userRepository;
    }

    public Team create(CreateTeamRequest request, String username) {
        if (teamRepository.existsByClassroomIdAndNameIgnoreCase(request.classroomId(), request.name().trim()))
            throw new DuplicateResourceException("Team name already exists in this classroom");
        User lecturer = requireUser(username);
        Team team = new Team();
        team.setName(request.name().trim());
        team.setDescription(clean(request.description()));
        team.setClassroomId(request.classroomId());
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
        return teamRepository.findDistinctByLecturerUsernameOrMembersUsernameOrderByUpdatedAtDesc(username, username);
    }

    @Transactional(readOnly = true)
    public List<Team> listByClassroom(Long classroomId) { return teamRepository.findByClassroomIdOrderByUpdatedAtDesc(classroomId); }

    @Transactional(readOnly = true)
    public Team get(Long id, String username) { return requireAccessible(id, username); }

    public Team update(Long id, UpdateTeamRequest request, String username) {
        Team team = requireOwned(id, username);
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
        User student = findMember(team, studentId);
        if (team.getLeader() != null && team.getLeader().getId().equals(studentId)) team.setLeader(null);
        team.getMembers().remove(student);
        return team;
    }

    public Team assignProject(Long id, Long projectId, String username) {
        Team team = requireOwned(id, username);
        team.setProjectId(projectId);
        return team;
    }

    public void delete(Long id, String username) { teamRepository.delete(requireOwned(id, username)); }

    @Transactional(readOnly = true)
    public List<User> availableStudents(Long classroomId) {
        return userRepository.findAll().stream()
                .filter(user -> user.isActive() && user.getRole() == RoleEnum.STUDENT)
                .filter(user -> !teamRepository.existsByClassroomIdAndMembersId(classroomId, user.getId()))
                .sorted(Comparator.comparing(User::getFullName, String.CASE_INSENSITIVE_ORDER)).toList();
    }

    private Team requireOwned(Long id, String username) {
        Team team = requireTeam(id);
        if (!team.getLecturer().getUsername().equals(username)) throw new BusinessRuleException("You cannot manage this team");
        return team;
    }
    private Team requireAccessible(Long id, String username) {
        Team team = requireTeam(id);
        boolean member = team.getMembers().stream().anyMatch(user -> user.getUsername().equals(username));
        if (!team.getLecturer().getUsername().equals(username) && !member) throw new BusinessRuleException("You cannot view this team");
        return team;
    }
    private Team requireTeam(Long id) { return teamRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Team not found")); }
    private User requireUser(String username) { return userRepository.findByUsername(username).orElseThrow(() -> new ResourceNotFoundException("User not found")); }
    private User requireAvailableStudent(Long id, Long classroomId) {
        User user = userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Student not found"));
        if (user.getRole() != RoleEnum.STUDENT || !user.isActive()) throw new BusinessRuleException("Only active students can join a team");
        if (teamRepository.existsByClassroomIdAndMembersId(classroomId, id)) throw new BusinessRuleException("Student already belongs to a team in this classroom");
        return user;
    }
    private User findMember(Team team, Long studentId) { return team.getMembers().stream().filter(user -> user.getId().equals(studentId)).findFirst().orElseThrow(() -> new ResourceNotFoundException("Team member not found")); }
    private String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
