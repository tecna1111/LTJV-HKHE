package com.cosre.cosre_backend.modules.project.controller;

import com.cosre.cosre_backend.common.dto.ApiResponse;
import com.cosre.cosre_backend.modules.project.service.MilestoneQuestionService;
import com.cosre.cosre_backend.modules.notification.service.NotificationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class MilestoneQuestionController {
    public record QuestionRequest(@NotBlank @Size(max = 1000) String questionText) {}
    public record AnswerRequest(@NotBlank @Size(max = 5000) String answerText) {}
    public record ReviewRequest(@NotNull @DecimalMin("0") @DecimalMax("10")
                                @Digits(integer = 2, fraction = 2) BigDecimal score,
                                @Size(max = 2000) String feedback) {}

    private final MilestoneQuestionService service;
    private final NotificationService notifications;
    public MilestoneQuestionController(MilestoneQuestionService service, NotificationService notifications) {
        this.service = service; this.notifications = notifications;
    }

    @GetMapping("/teams/{teamId}/milestones/{milestoneId}/questions")
    @PreAuthorize("hasAnyRole('LECTURER','STUDENT')")
    public ApiResponse<List<MilestoneQuestionService.QuestionView>> questions(@PathVariable Long teamId,
                                                                                @PathVariable Long milestoneId) {
        return new ApiResponse<>(true, "Questions loaded", service.list(teamId, milestoneId));
    }
    @PostMapping("/milestones/{milestoneId}/questions")
    @PreAuthorize("hasRole('LECTURER')")
    public ApiResponse<MilestoneQuestionService.QuestionView> create(@PathVariable Long milestoneId,
                                                                       @Valid @RequestBody QuestionRequest request) {
        return new ApiResponse<>(true, "Question created", service.create(milestoneId, request.questionText()));
    }
    @PutMapping("/milestone-questions/{questionId}")
    @PreAuthorize("hasRole('LECTURER')")
    public ApiResponse<MilestoneQuestionService.QuestionView> update(@PathVariable Long questionId,
                                                                       @Valid @RequestBody QuestionRequest request) {
        return new ApiResponse<>(true, "Question updated", service.update(questionId, request.questionText()));
    }
    @DeleteMapping("/milestone-questions/{questionId}")
    @PreAuthorize("hasRole('LECTURER')")
    public ApiResponse<Void> delete(@PathVariable Long questionId) {
        service.delete(questionId);
        return new ApiResponse<>(true, "Question deleted", null);
    }
    @GetMapping("/teams/{teamId}/milestone-questions/{questionId}/answers")
    @PreAuthorize("hasAnyRole('LECTURER','STUDENT')")
    public ApiResponse<List<MilestoneQuestionService.AnswerView>> answers(@PathVariable Long teamId,
                                                                            @PathVariable Long questionId) {
        return new ApiResponse<>(true, "Answers loaded", service.answers(teamId, questionId));
    }
    @PostMapping("/teams/{teamId}/milestone-questions/{questionId}/answers")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<MilestoneQuestionService.AnswerView> submit(@PathVariable Long teamId,
                                                                     @PathVariable Long questionId,
                                                                     @Valid @RequestBody AnswerRequest request,
                                                                     Authentication auth) {
        var result = service.submit(teamId, questionId, request.answerText());
        notifications.notifyTeamEvent(teamId, auth.getName(),
                "MILESTONE_ANSWER:" + result.id() + ":" + java.util.UUID.randomUUID(),
                "MILESTONE_ANSWER", "Câu trả lời cột mốc mới", "Một sinh viên đã nộp câu trả lời.",
                "/teams/" + teamId + "/milestone-questions");
        return new ApiResponse<>(true, "Answer saved", result);
    }
    @PutMapping("/teams/{teamId}/milestone-answers/{answerId}/review")
    @PreAuthorize("hasRole('LECTURER')")
    public ApiResponse<MilestoneQuestionService.AnswerView> review(@PathVariable Long teamId,
                                                                     @PathVariable Long answerId,
                                                                     @Valid @RequestBody ReviewRequest request) {
        var result = service.review(teamId, answerId, request.score(), request.feedback());
        notifications.notifyUserEvent(result.studentId(), "MILESTONE_ANSWER_REVIEW:" + result.id() + ":" + result.reviewedAt(),
                "MILESTONE_ANSWER_REVIEW", "Câu trả lời đã được đánh giá",
                "Giảng viên đã chấm điểm và phản hồi câu trả lời của bạn.",
                "/teams/" + teamId + "/milestone-questions");
        return new ApiResponse<>(true, "Answer reviewed", result);
    }
}
