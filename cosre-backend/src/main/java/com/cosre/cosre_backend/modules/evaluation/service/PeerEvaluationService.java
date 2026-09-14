package com.cosre.cosre_backend.modules.evaluation.service;

import com.cosre.cosre_backend.modules.evaluation.dto.request.PeerEvaluationSubmitRequest;
import com.cosre.cosre_backend.modules.evaluation.dto.response.PeerEvaluationResponse;
import com.cosre.cosre_backend.modules.evaluation.dto.response.StudentEvaluationSummaryResponse;

import java.util.List;

public interface PeerEvaluationService {

    /**
     * Nộp (hoặc nộp lại nếu chưa LOCKED) một bài đánh giá chéo.
     */
    PeerEvaluationResponse submit(Long evaluatorId, PeerEvaluationSubmitRequest request);

    /**
     * Các bài mà evaluator đã chấm cho người khác.
     */
    List<PeerEvaluationResponse> getGivenEvaluations(Long evaluatorId, Long projectId);

    /**
     * Các bài mà người khác đã chấm cho evaluatee.
     */
    List<PeerEvaluationResponse> getReceivedEvaluations(Long evaluateeId, Long projectId);

    /**
     * Điểm tổng hợp (trung bình) của 1 sinh viên trong 1 nhóm — dùng cho giảng viên xem.
     */
    StudentEvaluationSummaryResponse getStudentSummary(Long studentId, Long teamId, Long projectId);

    /**
     * Điểm tổng hợp của tất cả thành viên trong 1 nhóm — dùng cho giảng viên xem toàn nhóm.
     */
    List<StudentEvaluationSummaryResponse> getTeamSummary(Long teamId, Long projectId, List<Long> memberIds);

    /**
     * Khóa toàn bộ bài đánh giá của 1 nhóm/milestone (giảng viên chốt điểm, không cho sửa nữa).
     */
    void lockEvaluations(Long teamId, Long projectId);
}
