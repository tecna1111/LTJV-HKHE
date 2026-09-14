package com.cosre.cosre_backend.modules.evaluation.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PeerEvaluationSubmitRequest {

    @NotNull(message = "projectId không được để trống")
    private Long projectId;

    // Cho phép null nếu là đánh giá tổng kết không gắn milestone cụ thể
    private Long milestoneId;

    @NotNull(message = "teamId không được để trống")
    private Long teamId;

    @NotNull(message = "evaluateeId không được để trống")
    private Long evaluateeId;

    @jakarta.validation.constraints.Size(max = 500)
    private String comment;

    @NotEmpty(message = "Phải chấm ít nhất 1 tiêu chí")
    @Valid
    private List<@NotNull DetailItem> details;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DetailItem {
        @NotNull(message = "criteriaId không được để trống")
        private Long criteriaId;

        @NotNull(message = "score không được để trống")
        @jakarta.validation.constraints.DecimalMin("0")
        @jakarta.validation.constraints.Digits(integer = 3, fraction = 2)
        private BigDecimal score;

        @jakarta.validation.constraints.Size(max = 500)
    private String comment;
    }
}
