package com.cosre.cosre_backend.modules.kanban.service;

import com.cosre.cosre_backend.common.constants.RoleEnum;
import com.cosre.cosre_backend.common.exception.ResourceNotFoundException;
import com.cosre.cosre_backend.modules.account.entity.User;
import com.cosre.cosre_backend.modules.account.repository.UserRepository;
import com.cosre.cosre_backend.modules.classroom.repository.ClassroomRepository;
import com.cosre.cosre_backend.modules.team.entity.Team;
import jakarta.persistence.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import java.util.Objects;
@Service @RequiredArgsConstructor
public class KanbanAccess {
    private final EntityManager em;
    private final UserRepository users;
    private final ClassroomRepository classrooms;
    public User actor(String username) {
        return users.findByUsername(username).filter(User::isActive)
            .orElseThrow(() -> new AccessDeniedException("Tài khoản không hoạt động"));
    }
    // All board writes share the team lock, including first board creation.
    public Team view(Long id, User actor) {
        Team team=em.find(Team.class,id,LockModeType.PESSIMISTIC_WRITE);
        if(team==null) throw new ResourceNotFoundException("Không tìm thấy nhóm");
        var classroom=classrooms.findDetailedById(team.getClassroomId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lớp của nhóm"));
        boolean lecturer=actor.getRole()==RoleEnum.LECTURER && team.getLecturer()!=null
            && Objects.equals(team.getLecturer().getId(),actor.getId())
            && classroom.getLecturers().stream().anyMatch(u->u.getId().equals(actor.getId()));
        boolean member=actor.getRole()==RoleEnum.STUDENT
            && team.getMembers().stream().anyMatch(u->u.getId().equals(actor.getId()))
            && classroom.getStudents().stream().anyMatch(u->u.getId().equals(actor.getId()));
        if(!lecturer && !member) throw new AccessDeniedException("Bạn không có quyền truy cập nhóm này");
        return team;
    }
    public boolean manager(Team team, User actor) {
        return actor.getRole()==RoleEnum.LECTURER || team.getLeader()!=null && Objects.equals(team.getLeader().getId(),actor.getId());
    }
    public void requireManager(Team team, User actor) {
        if(!manager(team,actor)) throw new AccessDeniedException("Chỉ giảng viên phụ trách hoặc trưởng nhóm được quản lý sprint");
    }
}
