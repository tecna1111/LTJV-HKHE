package com.cosre.cosre_backend.modules.kanban.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
@Entity @Table(name="kanban_subtasks") @Getter @Setter
public class KanbanSubtask {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false) private Long taskId;
    @Column(nullable=false,length=200) private String title;
    @Column(nullable=false) private boolean done;
}
