package com.cosre.cosre_backend.modules.checkpoint.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "checkpoint_submissions")
@Getter
@Setter
@NoArgsConstructor
public class CheckpointSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "checkpoint_id", nullable = false)
    private Long checkpointId;

    @Column(name = "submitted_by", nullable = false)
    private Long submittedBy;

    @Column(length = 5000)
    private String content;

    @Column(name = "attachment_url", length = 1000)
    private String attachmentUrl;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    @Column(precision = 5, scale = 2)
    private BigDecimal score;

    @Column(length = 2000)
    private String feedback;

    @Column(name = "reviewed_by")
    private Long reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @PrePersist
    void onCreate() {
        submittedAt = LocalDateTime.now();
    }
}