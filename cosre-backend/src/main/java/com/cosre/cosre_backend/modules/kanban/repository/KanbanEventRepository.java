package com.cosre.cosre_backend.modules.kanban.repository;
import com.cosre.cosre_backend.modules.kanban.entity.KanbanEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface KanbanEventRepository extends JpaRepository<KanbanEvent,Long> {
    void deleteByBoardId(Long boardId);
    List<KanbanEvent> findTop100ByBoardIdOrderByIdDesc(Long boardId);
}
