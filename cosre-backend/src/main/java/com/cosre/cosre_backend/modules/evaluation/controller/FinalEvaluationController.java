package com.cosre.cosre_backend.modules.evaluation.controller;
import com.cosre.cosre_backend.common.dto.ApiResponse;
import com.cosre.cosre_backend.modules.evaluation.entity.*;
import com.cosre.cosre_backend.modules.evaluation.service.FinalEvaluationService;
import com.cosre.cosre_backend.modules.notification.service.NotificationService;
import com.cosre.cosre_backend.modules.project.repository.MilestoneAnswerRepository;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import java.util.List;
@RestController @RequestMapping("/api/v1/evaluations") @RequiredArgsConstructor
public class FinalEvaluationController {
    private final FinalEvaluationService service;
    private final NotificationService notifications;
    private final MilestoneAnswerRepository milestoneAnswers;
    @PutMapping("/final") @PreAuthorize("hasRole('LECTURER')")
    public ApiResponse<FinalEvaluation> grade(@Valid @RequestBody FinalEvaluationService.GradeRequest request) {
        var result = service.grade(request);
        String eventId = "FINAL_EVALUATION:" + result.getId() + ":" + result.getUpdatedAt();
        if (request.studentId() == null) notifications.notifyTeamEvent(request.teamId(), "", eventId,
                "FINAL_EVALUATION", "Nhóm đã được đánh giá", "Giảng viên đã chấm điểm cuối kỳ cho nhóm.",
                "/evaluations/final");
        else notifications.notifyUserEvent(request.studentId(), eventId, "FINAL_EVALUATION",
                "Bạn đã được đánh giá", "Giảng viên đã chấm điểm cuối kỳ cho bạn.", "/evaluations/final");
        return ApiResponse.success(result);
    }
    @GetMapping("/final")
    public ApiResponse<List<FinalEvaluation>> list(@RequestParam Long teamId, @RequestParam Long projectId) {
        return ApiResponse.success(service.list(teamId, projectId));
    }
    @PutMapping("/answers/{answerId}/peer-feedback") @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<AnswerPeerFeedback> feedback(@PathVariable Long answerId, @Valid @RequestBody FinalEvaluationService.FeedbackRequest request) {
        var result = service.feedback(answerId, request);
        var answer = milestoneAnswers.findById(answerId).orElseThrow();
        notifications.notifyUserEvent(answer.getStudentId(),
                "ANSWER_PEER_FEEDBACK:" + result.getId() + ":" + result.getUpdatedAt(),
                "ANSWER_PEER_FEEDBACK", "Câu trả lời có đánh giá chéo mới",
                "Một thành viên nhóm đã phản hồi câu trả lời của bạn.",
                "/teams/" + answer.getTeamId() + "/milestone-questions");
        return ApiResponse.success(result);
    }
    @GetMapping("/answers/{answerId}/peer-feedback")
    public ApiResponse<List<AnswerPeerFeedback>> feedbacks(@PathVariable Long answerId) {
        return ApiResponse.success(service.feedbacks(answerId));
    }
}
