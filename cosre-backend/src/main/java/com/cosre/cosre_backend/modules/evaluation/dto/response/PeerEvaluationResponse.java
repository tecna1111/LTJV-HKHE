package com.cosre.cosre_backend.modules.evaluation.dto.response;

import com.cosre.cosre_backend.modules.evaluation.entity.EvaluationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PeerEvaluationResponse {
    private Long id;
    private Long projectId;
    private Long milestoneId;
    private Long teamId;
    private Long evaluatorId;
    private Long evaluateeId;
    private EvaluationStatus status;
    private BigDecimal totalScore;
    private String comment;
    private LocalDateTime submittedAt;
    private List<DetailResponse> details;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DetailResponse {
        private Long criteriaId;
        private String criteriaTitle;
        private BigDecimal score;
        private BigDecimal maxScore;
        private String comment;
    }
}
