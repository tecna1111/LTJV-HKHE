package com.cosre.cosre_backend.modules.chat.service;

import com.cosre.cosre_backend.common.constants.RoleEnum;
import com.cosre.cosre_backend.common.exception.ResourceNotFoundException;
import com.cosre.cosre_backend.modules.account.repository.UserRepository;
import com.cosre.cosre_backend.modules.chat.entity.ChatRoomType;
import com.cosre.cosre_backend.modules.classroom.repository.ClassroomRepository;
import com.cosre.cosre_backend.modules.team.repository.TeamRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ChatRoomAccessServiceImpl implements ChatRoomAccessService {
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final ClassroomRepository classroomRepository;

    public ChatRoomAccessServiceImpl(UserRepository userRepository, TeamRepository teamRepository,
            ClassroomRepository classroomRepository) {
        this.userRepository = userRepository;
        this.teamRepository = teamRepository;
        this.classroomRepository = classroomRepository;
    }

    @Override
    public boolean canAccess(String username, ChatRoomType roomType, Long roomId) {
        var user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (!user.isActive()) return false;
        if (roomType == ChatRoomType.TEAM) {
            var team = teamRepository.findById(roomId)
                    .orElseThrow(() -> new ResourceNotFoundException("Team not found"));
            return team.getLecturer().getId().equals(user.getId())
                    || team.getMembers().stream().anyMatch(member -> member.getId().equals(user.getId()));
        }
        var classroom = classroomRepository.findDetailedById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Classroom not found"));
        boolean privileged = user.getRole() == RoleEnum.ADMIN || user.getRole() == RoleEnum.STAFF
                || user.getRole() == RoleEnum.HEAD_DEPT;
        return privileged || classroom.getLecturers().stream().anyMatch(member -> member.getId().equals(user.getId()))
                || classroom.getStudents().stream().anyMatch(member -> member.getId().equals(user.getId()));
    }

    @Override
    public void requireAccess(String username, ChatRoomType roomType, Long roomId) {
        if (!canAccess(username, roomType, roomId)) throw new AccessDeniedException("You cannot access this chat room");
    }
}
