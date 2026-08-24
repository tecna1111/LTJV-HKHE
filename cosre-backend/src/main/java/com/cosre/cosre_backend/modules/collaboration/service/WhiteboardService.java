package com.cosre.cosre_backend.modules.collaboration.service;
import com.cosre.cosre_backend.common.exception.ResourceNotFoundException;
import com.cosre.cosre_backend.modules.account.entity.User;
import com.cosre.cosre_backend.modules.account.repository.UserRepository;
import com.cosre.cosre_backend.modules.collaboration.dto.SaveWhiteboardRequest;
import com.cosre.cosre_backend.modules.collaboration.dto.WhiteboardResponse;
import com.cosre.cosre_backend.modules.collaboration.entity.Whiteboard;
import com.cosre.cosre_backend.modules.collaboration.repository.WhiteboardRepository;
import com.cosre.cosre_backend.modules.team.service.TeamAccessService;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
@Transactional
public class WhiteboardService {

    private final WhiteboardRepository whiteboardRepository;
    private final UserRepository userRepository;
    private final TeamAccessService teamAccessService;
    public WhiteboardService(WhiteboardRepository whiteboardRepository, UserRepository userRepository,
            TeamAccessService teamAccessService) {
        this.whiteboardRepository = whiteboardRepository;
        this.userRepository = userRepository;
        this.teamAccessService = teamAccessService;
    }

    @Transactional(readOnly = true)
    public WhiteboardResponse getByTeam(Long teamId, String username) {
        teamAccessService.requireViewAccess(teamId, username);
        return whiteboardRepository.findByTeamId(teamId)
                .map(WhiteboardResponse::from)
                .orElseGet(() -> WhiteboardResponse.empty(teamId));
    }

    public WhiteboardResponse save(Long teamId, String username, SaveWhiteboardRequest request) {
        teamAccessService.requireViewAccess(teamId, username);
        User actor = requireUser(username);

        Whiteboard whiteboard = whiteboardRepository.findByTeamId(teamId).orElse(null);

        if (whiteboard == null) {
            whiteboard = new Whiteboard();
            whiteboard.setTeamId(teamId);
        } else if (!whiteboard.getVersion().equals(request.version())) {
            throw new ObjectOptimisticLockingFailureException(Whiteboard.class, whiteboard.getId());
        }

        whiteboard.setCanvasData(request.canvasData());
        whiteboard.setUpdatedBy(actor.getId());

        return WhiteboardResponse.from(whiteboardRepository.save(whiteboard));
    }

    private User requireUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}