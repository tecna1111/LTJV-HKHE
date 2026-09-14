package com.cosre.cosre_backend.modules.evaluation.repository;

import com.cosre.cosre_backend.modules.evaluation.entity.EvaluationStatus;
import com.cosre.cosre_backend.modules.evaluation.entity.PeerEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PeerEvaluationRepository extends JpaRepository<PeerEvaluation, Long> {

    // Các bài mà 1 sinh viên đã CHẤM cho người khác (given)
    List<PeerEvaluation> findByEvaluatorIdAndProjectId(Long evaluatorId, Long projectId);

    // Các bài mà 1 sinh viên ĐƯỢC chấm bởi người khác (received)
    List<PeerEvaluation> findByEvaluateeIdAndProjectId(Long evaluateeId, Long projectId);

    List<PeerEvaluation> findByTeamIdAndProjectId(Long teamId, Long projectId);

    List<PeerEvaluation> findByEvaluateeIdAndTeamIdAndProjectIdAndStatus(
            Long evaluateeId, Long teamId, Long projectId, EvaluationStatus status);

    // Dùng để kiểm tra đã tồn tại bài đánh giá cho cặp evaluator-evaluatee-milestone chưa,
    // phục vụ upsert khi sinh viên sửa lại bài đã nộp (miễn là chưa LOCKED).
    Optional<PeerEvaluation> findByEvaluatorIdAndEvaluateeIdAndProjectIdAndMilestoneId(
            Long evaluatorId, Long evaluateeId, Long projectId, Long milestoneId);
}
