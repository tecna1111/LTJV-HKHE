package com.cosre.cosre_backend.modules.kanban.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
@Entity @Table(name="kanban_tasks") @Getter @Setter
public class KanbanTask {
    public enum Status { TODO, IN_PROGRESS, DONE }
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false) private Long boardId;
    @Column(nullable=false,length=200) private String title;
    @Column(length=4000) private String description;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private Status status=Status.TODO;
    @Column(nullable=false,precision=10,scale=2) private BigDecimal weight;
    private LocalDate dueOn;
    @Column(nullable=false) private int position;
    private Long sprintId;
    private Long milestoneId;
    private Long checkpointId;
    @Column(nullable=false) private Long createdBy;
    @ElementCollection @CollectionTable(name="kanban_assignees",joinColumns=@JoinColumn(name="task_id"))
    @Column(name="user_id") private Set<Long> assigneeIds=new LinkedHashSet<>();
}
