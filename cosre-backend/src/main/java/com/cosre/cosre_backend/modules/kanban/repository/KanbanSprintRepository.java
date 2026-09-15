package com.cosre.cosre_backend.modules.kanban.repository;
import com.cosre.cosre_backend.modules.kanban.entity.KanbanSprint;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface KanbanSprintRepository extends JpaRepository<KanbanSprint,Long> {
    List<KanbanSprint> findByBoardIdOrderByStartsOnAsc(Long boardId);
}
