package com.cosre.cosre_backend.modules.evaluation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CriteriaResponse {
    private Long id;
    private Long projectId;
    private String title;
    private String description;
    private BigDecimal maxScore;
    private BigDecimal weight;
    private LocalDateTime createdAt;
}
