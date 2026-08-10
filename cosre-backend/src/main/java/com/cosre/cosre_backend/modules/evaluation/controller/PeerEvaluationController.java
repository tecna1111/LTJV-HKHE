package com.cosre.cosre_backend.modules.evaluation.controller;

import com.cosre.cosre_backend.common.response.ApiResponse;
import com.cosre.cosre_backend.common.utils.SecurityUtils;
import com.cosre.cosre_backend.modules.evaluation.dto.request.PeerEvaluationSubmitRequest;
import com.cosre.cosre_backend.modules.evaluation.dto.response.PeerEvaluationResponse;
import com.cosre.cosre_backend.modules.evaluation.dto.response.StudentEvaluationSummaryResponse;
import com.cosre.cosre_backend.modules.evaluation.service.PeerEvaluationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/evaluations/peer")
@RequiredArgsConstructor
public class PeerEvaluationController {

    private final PeerEvaluationService peerEvaluationService;

    // Sinh viên nộp (hoặc nộp lại) bài đánh giá chéo cho 1 thành viên trong nhóm
    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<PeerEvaluationResponse> submit(@Valid @RequestBody PeerEvaluationSubmitRequest request) {
        Long evaluatorId = SecurityUtils.getCurrentUserId();
        return ApiResponse.success(peerEvaluationService.submit(evaluatorId, request), "Nộp đánh giá thành công");
    }

    // Các bài mà chính mình đã chấm cho người khác
    @GetMapping("/given")
    public ApiResponse<List<PeerEvaluationResponse>> getGiven(@RequestParam Long projectId) {
        Long evaluatorId = SecurityUtils.getCurrentUserId();
        return ApiResponse.success(peerEvaluationService.getGivenEvaluations(evaluatorId, projectId));
    }

    // Các bài mà người khác đã chấm cho mình
    @GetMapping("/received")
    public ApiResponse<List<PeerEvaluationResponse>> getReceived(@RequestParam Long projectId) {
        Long evaluateeId = SecurityUtils.getCurrentUserId();
        return ApiResponse.success(peerEvaluationService.getReceivedEvaluations(evaluateeId, projectId));
    }

    // Điểm tổng hợp của 1 sinh viên cụ thể trong nhóm — giảng viên xem để chấm điểm
    @GetMapping("/summary/student/{studentId}")
    @PreAuthorize("hasRole('LECTURER')")
    public ApiResponse<StudentEvaluationSummaryResponse> getStudentSummary(
            @PathVariable Long studentId,
            @RequestParam Long teamId,
            @RequestParam Long projectId) {
        return ApiResponse.success(peerEvaluationService.getStudentSummary(studentId, teamId, projectId));
    }

    // Điểm tổng hợp của toàn bộ thành viên trong nhóm
    @GetMapping("/summary/team/{teamId}")
    @PreAuthorize("hasRole('LECTURER')")
    public ApiResponse<List<StudentEvaluationSummaryResponse>> getTeamSummary(
            @PathVariable Long teamId,
            @RequestParam Long projectId,
            @RequestParam List<Long> memberIds) {
        return ApiResponse.success(peerEvaluationService.getTeamSummary(teamId, projectId, memberIds));
    }

    // Giảng viên chốt điểm — khóa toàn bộ đánh giá của nhóm, không cho sửa nữa
    @PostMapping("/lock")
    @PreAuthorize("hasRole('LECTURER')")
    public ApiResponse<Void> lock(@RequestParam Long teamId, @RequestParam Long projectId) {
        peerEvaluationService.lockEvaluations(teamId, projectId);
        return ApiResponse.success(null, "Đã khóa đánh giá của nhóm");
    }
}
