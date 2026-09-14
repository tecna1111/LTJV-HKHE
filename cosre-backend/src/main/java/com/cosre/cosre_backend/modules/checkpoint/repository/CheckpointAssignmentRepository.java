package com.cosre.cosre_backend.modules.checkpoint.repository;

import com.cosre.cosre_backend.modules.checkpoint.entity.CheckpointAssignment;
import com.cosre.cosre_backend.modules.checkpoint.entity.CheckpointAssignmentId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CheckpointAssignmentRepository
        extends JpaRepository<CheckpointAssignment, CheckpointAssignmentId> {

    List<CheckpointAssignment> findByCheckpointId(Long checkpointId);

    boolean existsByCheckpointIdAndStudentId(
            Long checkpointId,
            Long studentId
    );

    void deleteByCheckpointId(Long checkpointId);
}