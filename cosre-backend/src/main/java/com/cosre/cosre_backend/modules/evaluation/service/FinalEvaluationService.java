package com.cosre.cosre_backend.modules.evaluation.service;

import com.cosre.cosre_backend.common.constants.RoleEnum;
import com.cosre.cosre_backend.common.exception.*;
import com.cosre.cosre_backend.modules.evaluation.entity.*;
import com.cosre.cosre_backend.modules.evaluation.repository.*;
import com.cosre.cosre_backend.modules.project.repository.*;
import com.cosre.cosre_backend.modules.project.entity.MilestoneAnswer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service @RequiredArgsConstructor @Transactional(readOnly = true)
public class FinalEvaluationService {
    private final EvaluationAccessService access;
    private final EvaluationRoundRepository rounds;
    private final FinalEvaluationRepository grades;
    private final AnswerPeerFeedbackRepository feedbacks;
    private final MilestoneAnswerRepository answers;
    private final MilestoneQuestionRepository questions;
    private final ProjectMilestoneRepository milestones;

    public record GradeRequest(@NotNull Long teamId, @NotNull Long projectId, Long studentId,
            @NotNull @DecimalMin("0") @DecimalMax("10") @Digits(integer=2, fraction=2) BigDecimal score,
            @Size(max=2000) String feedback) {}
    public record FeedbackRequest(@NotBlank @Size(max=2000) String feedback) {}

    private void requireOpen(Long teamId, Long projectId, boolean finalRequired) {
        var round = rounds.findById(teamId + ":" + projectId).orElse(null);
        if (round != null && round.isLocked()) throw new BusinessRuleException("Evaluation round is locked");
        if (finalRequired && (round == null || !round.isFinalOpen()))
            throw new BusinessRuleException("Final evaluation is not open");
    }

    @Transactional
    public FinalEvaluation grade(GradeRequest request) {
        access.lockProject(request.projectId());
        var team = access.lecturer(request.teamId(), request.projectId());
        if (request.studentId() != null) access.member(team, request.studentId());
        requireOpen(request.teamId(), request.projectId(), true);
        String id = request.teamId() + ":" + request.projectId() + ":" + (request.studentId() == null ? "team" : request.studentId());
        var grade = grades.findById(id).orElseGet(FinalEvaluation::new);
        grade.setId(id); grade.setTeamId(request.teamId()); grade.setProjectId(request.projectId());
        grade.setStudentId(request.studentId()); grade.setLecturerId(access.currentUser().getId());
        grade.setScore(request.score()); grade.setFeedback(request.feedback()); grade.setUpdatedAt(LocalDateTime.now());
        return grades.save(grade);
    }

    public List<FinalEvaluation> list(Long teamId, Long projectId) {
        var team = access.team(teamId, projectId);
        var user = access.currentUser();
        if (team.getLecturer().getId().equals(user.getId())) return grades.findByTeamIdAndProjectId(teamId, projectId);
        access.member(team, user.getId());
        return grades.findByTeamIdAndProjectId(teamId, projectId).stream()
                .filter(g -> g.getStudentId() == null || g.getStudentId().equals(user.getId())).toList();
    }

    private Long answerProject(MilestoneAnswer answer) {
        var question = questions.findById(answer.getQuestionId()).orElseThrow(() -> new ResourceNotFoundException("Question not found"));
        return milestones.findById(question.getMilestoneId()).orElseThrow(() -> new ResourceNotFoundException("Milestone not found")).getProject().getId();
    }

    @Transactional
    public AnswerPeerFeedback feedback(Long answerId, FeedbackRequest request) {
        var answer = answers.findById(answerId).orElseThrow(() -> new ResourceNotFoundException("Answer not found"));
        Long projectId = answerProject(answer);
        access.lockProject(projectId);
        var team = access.team(answer.getTeamId(), projectId);
        var user = access.currentUser();
        access.member(team, user.getId());
        access.member(team, answer.getStudentId());
        if (user.getRole() != RoleEnum.STUDENT || user.getId().equals(answer.getStudentId()))
            throw new AccessDeniedException("Cannot give peer feedback on your own answer");
        requireOpen(team.getId(), projectId, false);
        var feedback = feedbacks.findByAnswerIdAndReviewerId(answerId, user.getId()).orElseGet(AnswerPeerFeedback::new);
        feedback.setAnswerId(answerId); feedback.setReviewerId(user.getId());
        feedback.setFeedback(request.feedback().trim()); feedback.setUpdatedAt(LocalDateTime.now());
        return feedbacks.save(feedback);
    }

    public List<AnswerPeerFeedback> feedbacks(Long answerId) {
        var answer = answers.findById(answerId).orElseThrow(() -> new ResourceNotFoundException("Answer not found"));
        var team = access.team(answer.getTeamId(), answerProject(answer));
        var user = access.currentUser();
        if (!team.getLecturer().getId().equals(user.getId())) access.member(team, user.getId());
        return feedbacks.findByAnswerId(answerId);
    }
}
