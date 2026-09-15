package com.cosre.cosre_backend.modules.evaluation.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
@Entity @Table(name = "answer_peer_feedback", uniqueConstraints = @UniqueConstraint(columnNames = {"answer_id", "reviewer_id"})) @Getter @Setter
public class AnswerPeerFeedback {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "answer_id", nullable = false) private Long answerId;
    @Column(name = "reviewer_id", nullable = false) private Long reviewerId;
    @Column(nullable = false, length = 2000) private String feedback;
    @Column(nullable = false) private LocalDateTime updatedAt;
}
