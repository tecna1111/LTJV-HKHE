package com.cosre.cosre_backend.modules.evaluation.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Entity @Table(name = "final_evaluations") @Getter @Setter
public class FinalEvaluation {
    @Id @Column(length = 100) private String id;
    @Column(nullable = false) private Long teamId;
    @Column(nullable = false) private Long projectId;
    private Long studentId;
    @Column(nullable = false) private Long lecturerId;
    @Column(nullable = false, precision = 5, scale = 2) private BigDecimal score;
    @Column(length = 2000) private String feedback;
    @Column(nullable = false) private LocalDateTime updatedAt;
}
