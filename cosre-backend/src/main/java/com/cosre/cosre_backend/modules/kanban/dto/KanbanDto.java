package com.cosre.cosre_backend.modules.kanban.dto;
import com.cosre.cosre_backend.modules.kanban.entity.KanbanTask;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
public final class KanbanDto {
    private KanbanDto() {}
    public record TaskInput(@NotNull @PositiveOrZero Long revision, @NotBlank @Size(max=200) String title,
        @Size(max=4000) String description, @NotNull @DecimalMin("0.01") @Digits(integer=8,fraction=2) BigDecimal weight,
        LocalDate dueOn, Long sprintId, Long milestoneId, Long checkpointId,
        @NotNull @Size(max=100) Set<@NotNull Long> assigneeIds) {}
    public record MoveInput(@NotNull @PositiveOrZero Long revision, @NotNull KanbanTask.Status status,
        @Min(0) int position) {}
    public record SprintInput(@NotNull @PositiveOrZero Long revision, @NotBlank @Size(max=200) String title,
        @NotNull LocalDate startsOn, @NotNull LocalDate endsOn) {}
    public record SubtaskInput(@NotNull @PositiveOrZero Long revision, @NotBlank @Size(max=200) String title, boolean done) {}
    public record Person(Long id, String name) {}
    public record Choice(Long id, String title, Long milestoneId) {}
    public record SubtaskView(Long id, String title, boolean done) {}
    public record TaskView(Long id, String title, String description, KanbanTask.Status status, BigDecimal weight,
        LocalDate dueOn, int position, Long sprintId, Long milestoneId, Long checkpointId, Set<Long> assigneeIds,
        List<SubtaskView> subtasks) {}
    public record SprintView(Long id, String title, LocalDate startsOn, LocalDate endsOn) {}
    public record Contribution(Long userId, String name, BigDecimal doneWeight, BigDecimal percent) {}
    public record Metrics(String formulaStatus, String description, BigDecimal totalWeight, BigDecimal doneWeight,
        BigDecimal progressPercent, List<Contribution> members) {}
    public record EventView(Long id, Long actorId, String action, String detail, LocalDateTime occurredAt) {}
    public record BoardView(Long id, Long teamId, String teamName, long revision, boolean canManage, boolean canEditTasks,
        List<Person> members, List<Choice> milestones, List<Choice> checkpoints,
        List<SprintView> sprints, List<TaskView> tasks, Metrics metrics, List<EventView> events) {}
}
