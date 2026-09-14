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
    private String title;

    private String description;

    @NotNull(message = "maxScore không được để trống")
    @DecimalMin(value = "0.0", inclusive = false, message = "maxScore phải lớn hơn 0")
    private BigDecimal maxScore;

    @NotNull(message = "weight không được để trống")
    @DecimalMin(value = "0.0", inclusive = false, message = "weight phải lớn hơn 0")
    @DecimalMax(value = "1.0", message = "weight không được vượt quá 1.0")
    private BigDecimal weight;
}
