package com.cosre.cosre_backend.modules.kanban.repository;
import com.cosre.cosre_backend.modules.kanban.entity.KanbanBoard;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface KanbanBoardRepository extends JpaRepository<KanbanBoard,Long> {
    Optional<KanbanBoard> findByTeamId(Long teamId);
}
