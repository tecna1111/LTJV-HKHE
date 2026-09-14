package com.cosre.cosre_backend.modules.team.service;

import com.cosre.cosre_backend.common.exception.BusinessRuleException;
import com.cosre.cosre_backend.common.exception.ResourceNotFoundException;
import com.cosre.cosre_backend.modules.account.entity.User;
import com.cosre.cosre_backend.modules.account.repository.UserRepository;
import com.cosre.cosre_backend.modules.team.entity.Team;
import com.cosre.cosre_backend.modules.team.repository.TeamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class TeamAccessService {

    private final TeamRepository teamRepository;
    private final UserRepository userRepository;

    public TeamAccessService(TeamRepository teamRepository, UserRepository userRepository) {
        this.teamRepository = teamRepository;
        this.userRepository = userRepository;
    }
    @Transactional(readOnly = true)
    public Team requireViewAccess(Long teamId, String username) {
        Team team = requireTeam(teamId);
        boolean isLecturer = team.getLecturer().getUsername().equals(username);
        boolean isMember = team.getMembers().stream()
                .anyMatch(member -> member.getUsername().equals(username));
        if (!isLecturer && !isMember) {
            throw new BusinessRuleException("You do not have access to this team");
        }
        return team;
    }

    @Transactional(readOnly = true)
    public Team requireLecturerAccess(Long teamId, String username) {
        Team team = requireTeam(teamId);
        if (!team.getLecturer().getUsername().equals(username)) {
            throw new BusinessRuleException("Only the managing lecturer can perform this action");
        }
        return team;
    }

    @Transactional(readOnly = true)
    public Team requireLeaderAccess(Long teamId, String username) {
        Team team = requireTeam(teamId);
        User user = requireUser(username);
        boolean isLeader = team.getLeader() != null && team.getLeader().getId().equals(user.getId());
        if (!isLeader) {
            throw new BusinessRuleException("Only the team leader can perform this action");
        }
        return team;
    }
    private Team requireTeam(Long id) {
        return teamRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found"));
    }

    private User requireUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}