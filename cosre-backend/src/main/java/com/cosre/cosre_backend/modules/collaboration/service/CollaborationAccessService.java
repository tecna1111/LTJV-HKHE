package com.cosre.cosre_backend.modules.collaboration.service;

import com.cosre.cosre_backend.common.exception.BusinessRuleException;
import com.cosre.cosre_backend.modules.account.repository.UserRepository;
import com.cosre.cosre_backend.modules.team.service.TeamAccessService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CollaborationAccessService {
    private final TeamAccessService teams;
    private final UserRepository users;
    public CollaborationAccessService(TeamAccessService teams, UserRepository users) {
        this.teams = teams; this.users = users;
    }
    @Transactional(readOnly = true)
    public void requireAccess(Long teamId, String username) {
        var user = users.findByUsername(username).orElseThrow(() -> new AccessDeniedException("Vui lòng đăng nhập"));
        if (!user.isActive() || !(user.getRole().name().equals("LECTURER") || user.getRole().name().equals("STUDENT")))
            throw new AccessDeniedException("Tài khoản không được sử dụng cộng tác");
        try { teams.requireViewAccess(teamId, username); }
        catch (BusinessRuleException e) { throw new AccessDeniedException("Bạn không có quyền truy cập nhóm này"); }
    }
}
