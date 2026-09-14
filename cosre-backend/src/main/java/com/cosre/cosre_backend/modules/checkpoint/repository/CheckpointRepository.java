package com.cosre.cosre_backend.modules.checkpoint.repository;

import com.cosre.cosre_backend.modules.checkpoint.entity.Checkpoint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CheckpointRepository extends JpaRepository<Checkpoint, Long> {
    List<Checkpoint> findByTeamIdOrderByDueAtAsc(Long teamId);
}