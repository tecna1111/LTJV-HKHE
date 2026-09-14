package com.cosre.cosre_backend.modules.evaluation.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity @Table(name = "evaluation_rounds") @Getter @Setter
public class EvaluationRound {
    @Id @Column(length = 64) private String id;
    @Column(nullable = false) private boolean finalOpen;
    @Column(nullable = false) private boolean locked;
}
