package com.cosre.cosre_backend.modules.evaluation.repository;

import com.cosre.cosre_backend.modules.evaluation.entity.EvaluationCriteria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EvaluationCriteriaRepository extends JpaRepository<EvaluationCriteria, Long> {

    List<EvaluationCriteria> findByProjectId(Long projectId);

    boolean existsByProjectIdAndTitleIgnoreCase(Long projectId, String title);
}
