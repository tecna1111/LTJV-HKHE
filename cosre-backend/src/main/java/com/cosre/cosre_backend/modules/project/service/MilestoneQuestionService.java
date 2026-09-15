package com.cosre.cosre_backend.modules.project.service;

import com.cosre.cosre_backend.common.constants.RoleEnum;
import com.cosre.cosre_backend.common.exception.BusinessRuleException;
import com.cosre.cosre_backend.common.exception.ResourceNotFoundException;
import com.cosre.cosre_backend.modules.evaluation.service.EvaluationAccessService;
import com.cosre.cosre_backend.modules.project.entity.MilestoneAnswer;
import com.cosre.cosre_backend.modules.project.entity.MilestoneQuestion;
import com.cosre.cosre_backend.modules.project.entity.ProjectMilestone;
import com.cosre.cosre_backend.modules.project.repository.MilestoneAnswerRepository;
import com.cosre.cosre_backend.modules.project.repository.MilestoneQuestionRepository;
import com.cosre.cosre_backend.modules.project.repository.ProjectMilestoneRepository;
import com.cosre.cosre_backend.modules.team.entity.Team;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class MilestoneQuestionService {
    public record QuestionView(Long id, Long milestoneId, String questionText, Long createdBy) {
        static QuestionView from(MilestoneQuestion q) {
            return new QuestionView(q.getId(), q.getMilestoneId(), q.getQuestionText(), q.getCreatedBy());
        }
    }
    public record AnswerView(Long id, Long questionId, Long teamId, Long studentId, String answerText,
                             String lecturerFeedback, BigDecimal score, LocalDateTime submittedAt,
                             LocalDateTime reviewedAt) {
        static AnswerView from(MilestoneAnswer a) {
            return new AnswerView(a.getId(), a.getQuestionId(), a.getTeamId(), a.getStudentId(),
                    a.getAnswerText(), a.getLecturerFeedback(), a.getScore(), a.getSubmittedAt(), a.getReviewedAt());
        }
    }

    private final ProjectMilestoneRepository milestones;
    private final MilestoneQuestionRepository questions;
    private final MilestoneAnswerRepository answers;
    private final EvaluationAccessService access;

    public MilestoneQuestionService(ProjectMilestoneRepository milestones, MilestoneQuestionRepository questions,
                                    MilestoneAnswerRepository answers, EvaluationAccessService access) {
        this.milestones = milestones;
        this.questions = questions;
        this.answers = answers;
        this.access = access;
    }

    private ProjectMilestone milestone(Long id) {
        return milestones.findById(id).orElseThrow(() -> new ResourceNotFoundException("Milestone not found"));
    }
    private MilestoneQuestion question(Long id) {
        return questions.findById(id).orElseThrow(() -> new ResourceNotFoundException("Question not found"));
    }
    private Team readableTeam(Long teamId, Long projectId) {
        Team team = access.team(teamId, projectId);
        var user = access.currentUser();
        if (team.getLecturer().getId().equals(user.getId())) return team;
        if (user.getRole() != RoleEnum.STUDENT) throw new AccessDeniedException("No access to team questions");
        access.member(team, user.getId());
        return team;
    }
    private void requireManagingLecturer(Long projectId) {
        if (access.currentUser().getRole() != RoleEnum.LECTURER)
            throw new AccessDeniedException("Only a lecturer can manage milestone questions");
        access.viewProject(projectId);
    }

    @Transactional(readOnly = true)
    public List<QuestionView> list(Long teamId, Long milestoneId) {
        var m = milestone(milestoneId);
        readableTeam(teamId, m.getProject().getId());
        return questions.findByMilestoneIdOrderByCreatedAtAsc(milestoneId).stream().map(QuestionView::from).toList();
    }

    public QuestionView create(Long milestoneId, String text) {
        var m = milestone(milestoneId);
        requireManagingLecturer(m.getProject().getId());
        var q = new MilestoneQuestion();
        q.setMilestoneId(milestoneId);
        q.setQuestionText(text.trim());
        q.setCreatedBy(access.currentUser().getId());
        return QuestionView.from(questions.save(q));
    }

    public QuestionView update(Long questionId, String text) {
        var q = question(questionId);
        requireManagingLecturer(milestone(q.getMilestoneId()).getProject().getId());
        if (answers.existsByQuestionId(questionId))
            throw new BusinessRuleException("Cannot edit a question after students have answered it");
        q.setQuestionText(text.trim());
        return QuestionView.from(q);
    }

    public void delete(Long questionId) {
        var q = question(questionId);
        requireManagingLecturer(milestone(q.getMilestoneId()).getProject().getId());
        if (answers.existsByQuestionId(questionId))
            throw new BusinessRuleException("Cannot delete a question after students have answered it");
        questions.delete(q);
    }

    @Transactional(readOnly = true)
    public List<AnswerView> answers(Long teamId, Long questionId) {
        var q = question(questionId);
        Team team = readableTeam(teamId, milestone(q.getMilestoneId()).getProject().getId());
        return answers.findByQuestionIdAndTeamIdOrderBySubmittedAtAsc(questionId, teamId).stream()
                .filter(a -> team.getMembers().stream().anyMatch(member -> member.getId().equals(a.getStudentId())))
                .map(AnswerView::from).toList();
    }

    public AnswerView submit(Long teamId, Long questionId, String text) {
        var q = question(questionId);
        Team team = readableTeam(teamId, milestone(q.getMilestoneId()).getProject().getId());
        var user = access.currentUser();
        if (user.getRole() != RoleEnum.STUDENT || team.getLecturer().getId().equals(user.getId()))
            throw new AccessDeniedException("Only team students can answer a question");
        var a = answers.findByQuestionIdAndTeamIdAndStudentId(questionId, teamId, user.getId())
                .orElseGet(MilestoneAnswer::new);
        if (a.getReviewedAt() != null) throw new BusinessRuleException("Reviewed answers cannot be changed");
        a.setQuestionId(questionId);
        a.setTeamId(teamId);
        a.setStudentId(user.getId());
        a.setAnswerText(text.trim());
        return AnswerView.from(answers.save(a));
    }

    public AnswerView review(Long teamId, Long answerId, BigDecimal score, String feedback) {
        var a = answers.findById(answerId).orElseThrow(() -> new ResourceNotFoundException("Answer not found"));
        if (!a.getTeamId().equals(teamId)) throw new ResourceNotFoundException("Answer not found in team");
        var q = question(a.getQuestionId());
        Team team = access.lecturer(teamId, milestone(q.getMilestoneId()).getProject().getId());
        access.member(team, a.getStudentId());
        a.setScore(score);
        a.setLecturerFeedback(feedback == null ? null : feedback.trim());
        a.setReviewedAt(LocalDateTime.now());
        a.setReviewedBy(access.currentUser().getId());
        return AnswerView.from(a);
    }
}
