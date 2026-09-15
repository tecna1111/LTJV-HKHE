package com.cosre.cosre_backend.modules.team.service;

import org.springframework.security.access.AccessDeniedException;
import com.cosre.cosre_backend.modules.team.entity.Team;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class TeamAccessService {

    private final TeamService teamService;

    public TeamAccessService(TeamService teamService) {
        this.teamService = teamService;
    }
    @Transactional(readOnly = true)
    public Team requireViewAccess(Long teamId, String username) {
        return teamService.get(teamId, username);
    }

    @Transactional(readOnly = true)
    public Team requireLecturerAccess(Long teamId, String username) {
        Team team = requireViewAccess(teamId, username);
        if (!team.getLecturer().getUsername().equals(username)) {
            throw new AccessDeniedException("Only the managing lecturer can perform this action");
        }
        return team;
    }

    @Transactional(readOnly = true)
    public Team requireLeaderAccess(Long teamId, String username) {
        Team team = requireViewAccess(teamId, username);
        boolean isLeader = team.getLeader() != null && team.getLeader().getUsername().equals(username);
        if (!isLeader) {
            throw new AccessDeniedException("Only the team leader can perform this action");
        }
        return team;
    }
}
