package com.cosre.cosre_backend.modules.kanban.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
@Entity @Table(name="kanban_events") @Getter @Setter
public class KanbanEvent {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false) private Long boardId;
    @Column(nullable=false) private Long actorId;
    @Column(nullable=false,length=40) private String action;
    @Column(nullable=false,length=12000) private String detail;
    @Column(nullable=false) private LocalDateTime occurredAt;
}
