package com.cosre.cosre_backend.modules.team.repository;

import com.cosre.cosre_backend.modules.team.entity.Team;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TeamRepository extends JpaRepository<Team, Long> {
    boolean existsByClassroomIdAndNameIgnoreCase(Long classroomId, String name);
    boolean existsByClassroomIdAndMembersId(Long classroomId, Long studentId);
    @EntityGraph(attributePaths = {"lecturer", "leader", "members"})
    List<Team> findDistinctByLecturerUsernameOrMembersUsernameOrderByUpdatedAtDesc(String lecturerUsername, String memberUsername);
    @EntityGraph(attributePaths = {"lecturer", "leader", "members"})
    List<Team> findByClassroomIdOrderByUpdatedAtDesc(Long classroomId);
}
