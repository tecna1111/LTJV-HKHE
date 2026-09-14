package com.cosre.cosre_backend.modules.evaluation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Điểm tổng hợp của 1 sinh viên trong 1 nhóm, tính trung bình từ tất cả các
 * bài đánh giá chéo mà thành viên khác đã chấm cho sinh viên đó.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentEvaluationSummaryResponse {
    private Long studentId;
    private Long teamId;
    private Long projectId;
    private BigDecimal averageScore;
    private int totalReviewsReceived;
    // key = criteriaId, value = điểm trung bình của tiêu chí đó
    private Map<Long, BigDecimal> criteriaAverageBreakdown;
    private List<PeerEvaluationResponse> receivedEvaluations;
}
