package com.cosre.cosre_backend.modules.evaluation.repository;
import com.cosre.cosre_backend.modules.evaluation.entity.AnswerPeerFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface AnswerPeerFeedbackRepository extends JpaRepository<AnswerPeerFeedback, Long> {
    Optional<AnswerPeerFeedback> findByAnswerIdAndReviewerId(Long answerId, Long reviewerId);
    List<AnswerPeerFeedback> findByAnswerId(Long answerId);
}
