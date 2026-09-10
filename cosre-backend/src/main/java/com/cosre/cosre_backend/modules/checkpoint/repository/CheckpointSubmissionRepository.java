package com.cosre.cosre_backend.modules.checkpoint.repository;

import com.cosre.cosre_backend.modules.checkpoint.entity.CheckpointSubmission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CheckpointSubmissionRepository
        extends JpaRepository<CheckpointSubmission, Long> {

    List<CheckpointSubmission>
        findByCheckpointIdOrderBySubmittedAtDesc(Long checkpointId);
}