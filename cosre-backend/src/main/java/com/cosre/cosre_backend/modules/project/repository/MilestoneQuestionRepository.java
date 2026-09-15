package com.cosre.cosre_backend.modules.project.repository;
import com.cosre.cosre_backend.modules.project.entity.MilestoneQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
public interface MilestoneQuestionRepository extends JpaRepository<MilestoneQuestion, Long> {
    java.util.List<MilestoneQuestion> findByMilestoneIdOrderByCreatedAtAsc(Long milestoneId);
}
