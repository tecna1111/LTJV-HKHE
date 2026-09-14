package com.cosre.cosre_backend.modules.evaluation.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Một bài đánh giá chéo: sinh viên (evaluator) chấm điểm 1 thành viên khác
 * trong nhóm (evaluatee), theo bộ tiêu chí của project/milestone tương ứng.
 */
@Entity
@Table(
    name = "peer_evaluation",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_peer_evaluation_unique",
        columnNames = {"evaluator_id", "evaluatee_id", "project_id", "milestone_id"}
    )
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PeerEvaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    // Milestone có thể null nếu đây là đánh giá tổng kết cuối kỳ (không gắn 1 milestone cụ thể)
    @Column(name = "milestone_id")
    private Long milestoneId;

    @Column(name = "team_id", nullable = false)
    private Long teamId;

    @Column(name = "evaluator_id", nullable = false)
    private Long evaluatorId;

    @Column(name = "evaluatee_id", nullable = false)
    private Long evaluateeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EvaluationStatus status;

    // Tổng điểm quy đổi theo weight của từng tiêu chí (0 - 10 hoặc 0 - 100 tùy quy ước hệ thống)
    @Column(name = "total_score")
    private BigDecimal totalScore;

    @Column(length = 1000)
    private String comment;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder.Default
    @OneToMany(mappedBy = "peerEvaluation", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<PeerEvaluationDetail> details = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = EvaluationStatus.DRAFT;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void addDetail(PeerEvaluationDetail detail) {
        details.add(detail);
        detail.setPeerEvaluation(this);
    }

    public void clearDetails() {
        details.forEach(d -> d.setPeerEvaluation(null));
        details.clear();
    }
}
