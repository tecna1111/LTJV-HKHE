package com.cosre.cosre_backend.modules.kanban.repository;
import com.cosre.cosre_backend.modules.kanban.entity.KanbanTask;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface KanbanTaskRepository extends JpaRepository<KanbanTask,Long> {
    List<KanbanTask> findByBoardIdOrderByPositionAscIdAsc(Long boardId);
}
