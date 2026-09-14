package com.cosre.cosre_backend.modules.evaluation.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CriteriaRequest {

    @NotNull(message = "projectId không được để trống")
    private Long projectId;

    @NotBlank(message = "title không được để trống")
    @jakarta.validation.constraints.Size(max = 150)
    private String title;

    @jakarta.validation.constraints.Size(max = 500)
    private String description;

    // Thang điểm tối đa cho riêng tiêu chí này (ví dụ 10 hoặc 100). Hệ thống sẽ tự quy đổi
    // điểm chấm (score/maxScore) rồi nhân weight để ra điểm tổng trên thang 10 chung,
    // nên maxScore chỉ cần dương và không vượt quá 100 để tránh nhập nhầm.
    @NotNull(message = "maxScore không được để trống")
    @DecimalMin(value = "0.0", inclusive = false, message = "maxScore phải lớn hơn 0")
    @DecimalMax(value = "100.0", message = "maxScore không được vượt quá 100")
    @jakarta.validation.constraints.Digits(integer = 3, fraction = 2)
    private BigDecimal maxScore;

    @NotNull(message = "weight không được để trống")
    @DecimalMin(value = "0.0", inclusive = false, message = "weight phải lớn hơn 0")
    @DecimalMax(value = "1.0", message = "weight không được vượt quá 1.0")
    @jakarta.validation.constraints.Digits(integer = 3, fraction = 2)
    private BigDecimal weight;
}
