package com.cosre.cosre_backend.modules.evaluation.controller;
import com.cosre.cosre_backend.common.dto.ApiResponse;
import com.cosre.cosre_backend.modules.evaluation.entity.*;
import com.cosre.cosre_backend.modules.evaluation.service.FinalEvaluationService;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import java.util.List;
@RestController @RequestMapping("/api/v1/evaluations") @RequiredArgsConstructor
public class FinalEvaluationController {
    private final FinalEvaluationService service;
    @PutMapping("/final") @PreAuthorize("hasRole('LECTURER')")
    public ApiResponse<FinalEvaluation> grade(@Valid @RequestBody FinalEvaluationService.GradeRequest request) {
        return ApiResponse.success(service.grade(request));
    }
    @GetMapping("/final")
    public ApiResponse<List<FinalEvaluation>> list(@RequestParam Long teamId, @RequestParam Long projectId) {
        return ApiResponse.success(service.list(teamId, projectId));
    }
    @PutMapping("/answers/{answerId}/peer-feedback") @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<AnswerPeerFeedback> feedback(@PathVariable Long answerId, @Valid @RequestBody FinalEvaluationService.FeedbackRequest request) {
        return ApiResponse.success(service.feedback(answerId, request));
    }
    @GetMapping("/answers/{answerId}/peer-feedback")
    public ApiResponse<List<AnswerPeerFeedback>> feedbacks(@PathVariable Long answerId) {
        return ApiResponse.success(service.feedbacks(answerId));
    }
}
