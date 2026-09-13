package com.cosre.cosre_backend.modules.checkpoint.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "checkpoint_assignments")
@IdClass(CheckpointAssignmentId.class)
@Getter
@Setter
@NoArgsConstructor
public class CheckpointAssignment {

    @Id
    @Column(name = "checkpoint_id")
    private Long checkpointId;

    @Id
    @Column(name = "student_id")
    private Long studentId;

    @Column(name = "assigned_at", nullable = false)
    private LocalDateTime assignedAt;

    @PrePersist
    void onCreate() {
        assignedAt = LocalDateTime.now();
    }
}