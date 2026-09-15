package com.cosre.cosre_backend.modules.evaluation.repository;
import com.cosre.cosre_backend.modules.evaluation.entity.FinalEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface FinalEvaluationRepository extends JpaRepository<FinalEvaluation, String> {
    List<FinalEvaluation> findByTeamIdAndProjectId(Long teamId, Long projectId);
}
