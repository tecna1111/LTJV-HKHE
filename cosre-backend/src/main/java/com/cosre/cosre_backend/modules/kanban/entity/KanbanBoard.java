package com.cosre.cosre_backend.modules.kanban.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
@Entity @Table(name="kanban_boards") @Getter @Setter
public class KanbanBoard {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false, unique=true) private Long teamId;
    @Column(nullable=false) private long revision;
}
