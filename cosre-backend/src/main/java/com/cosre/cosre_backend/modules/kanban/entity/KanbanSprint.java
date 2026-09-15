package com.cosre.cosre_backend.modules.kanban.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
@Entity @Table(name="kanban_sprints") @Getter @Setter
public class KanbanSprint {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false) private Long boardId;
    @Column(nullable=false,length=200) private String title;
    @Column(nullable=false) private LocalDate startsOn;
    @Column(nullable=false) private LocalDate endsOn;
}
