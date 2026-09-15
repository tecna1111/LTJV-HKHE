package com.cosre.cosre_backend.modules.project.repository;
import com.cosre.cosre_backend.modules.project.entity.MilestoneAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
public interface MilestoneAnswerRepository extends JpaRepository<MilestoneAnswer, Long> {
    java.util.List<MilestoneAnswer> findByQuestionIdAndTeamIdOrderBySubmittedAtAsc(Long questionId, Long teamId);
    java.util.Optional<MilestoneAnswer> findByQuestionIdAndTeamIdAndStudentId(Long questionId, Long teamId, Long studentId);
    boolean existsByQuestionId(Long questionId);
}
