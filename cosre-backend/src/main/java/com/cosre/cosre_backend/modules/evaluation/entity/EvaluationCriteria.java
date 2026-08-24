package com.cosre.cosre_backend.modules.evaluation.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Tiêu chí đánh giá (rubric item) do giảng viên định nghĩa cho một dự án.
 * Ví dụ: "Đóng góp code", "Tinh thần hợp tác", "Đúng deadline"...
 */
@Entity
@Table(name = "evaluation_criteria")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationCriteria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Tham chiếu tới Project (module project). Không map @ManyToOne trực tiếp
    // để tránh phụ thuộc chặt giữa các module — chỉ lưu id.
    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(length = 500)
    private String description;

    @Column(name = "max_score", nullable = false)
    private BigDecimal maxScore;

    // Trọng số của tiêu chí này trong tổng điểm (0.0 - 1.0). Tổng các weight
    // của các tiêu chí thuộc cùng 1 project nên = 1.0, được validate ở service.
    @Column(nullable = false)
    private BigDecimal weight;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
