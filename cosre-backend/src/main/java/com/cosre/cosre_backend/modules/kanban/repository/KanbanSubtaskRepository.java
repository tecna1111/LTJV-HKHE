package com.cosre.cosre_backend.modules.kanban.repository;
import com.cosre.cosre_backend.modules.kanban.entity.KanbanSubtask;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface KanbanSubtaskRepository extends JpaRepository<KanbanSubtask,Long> {
    List<KanbanSubtask> findByTaskIdOrderByIdAsc(Long taskId);
    void deleteByTaskId(Long taskId);
}
