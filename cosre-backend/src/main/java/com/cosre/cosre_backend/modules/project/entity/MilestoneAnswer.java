package com.cosre.cosre_backend.modules.project.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "milestone_answers")
@Getter
@Setter
@NoArgsConstructor
public class MilestoneAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "question_id", nullable = false)
    private Long questionId;

    @Column(name = "team_id", nullable = false)
    private Long teamId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "answer_text", nullable = false, length = 5000)
    private String answerText;

    @Column(name = "lecturer_feedback", length = 2000)
    private String lecturerFeedback;

    @Column(precision = 5, scale = 2)
    private BigDecimal score;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "reviewed_by")
    private Long reviewedBy;

    @PrePersist
    void onCreate() {
        submittedAt = LocalDateTime.now();
        updatedAt = submittedAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}