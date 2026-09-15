package com.cosre.cosre_backend.modules.kanban.controller;
import com.cosre.cosre_backend.modules.kanban.service.KanbanService;
import com.cosre.cosre_backend.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import static com.cosre.cosre_backend.modules.kanban.dto.KanbanDto.*;
@RestController @RequestMapping("/api/v1/teams/{teamId}/kanban")
@PreAuthorize("hasAnyRole('LECTURER','STUDENT')") @RequiredArgsConstructor
public class KanbanController {
    private final KanbanService service;
    private <T> ApiResponse<T> ok(T value) {return new ApiResponse<>(true,"Kanban",value);}
    @GetMapping public ApiResponse<BoardView> board(@PathVariable Long teamId,Authentication a){return ok(service.board(teamId,a.getName()));}
    @GetMapping("/metrics") public ApiResponse<Metrics> metrics(@PathVariable Long teamId,Authentication a){return ok(service.metrics(teamId,a.getName()));}
    @PostMapping("/tasks") public ApiResponse<BoardView> create(@PathVariable Long teamId,@Valid @RequestBody TaskInput r,Authentication a){return ok(service.saveTask(teamId,null,r,a.getName()));}
    @PutMapping("/tasks/{id}") public ApiResponse<BoardView> update(@PathVariable Long teamId,@PathVariable Long id,@Valid @RequestBody TaskInput r,Authentication a){return ok(service.saveTask(teamId,id,r,a.getName()));}
    @PutMapping("/tasks/{id}/move") public ApiResponse<BoardView> move(@PathVariable Long teamId,@PathVariable Long id,@Valid @RequestBody MoveInput r,Authentication a){return ok(service.move(teamId,id,r,a.getName()));}
    @DeleteMapping("/tasks/{id}") public ApiResponse<BoardView> delete(@PathVariable Long teamId,@PathVariable Long id,@RequestParam Long revision,Authentication a){return ok(service.deleteTask(teamId,id,revision,a.getName()));}
    @PostMapping("/sprints") public ApiResponse<BoardView> createSprint(@PathVariable Long teamId,@Valid @RequestBody SprintInput r,Authentication a){return ok(service.saveSprint(teamId,null,r,a.getName()));}
    @PutMapping("/sprints/{id}") public ApiResponse<BoardView> updateSprint(@PathVariable Long teamId,@PathVariable Long id,@Valid @RequestBody SprintInput r,Authentication a){return ok(service.saveSprint(teamId,id,r,a.getName()));}
    @DeleteMapping("/sprints/{id}") public ApiResponse<BoardView> deleteSprint(@PathVariable Long teamId,@PathVariable Long id,@RequestParam Long revision,Authentication a){return ok(service.deleteSprint(teamId,id,revision,a.getName()));}
    @PostMapping("/tasks/{taskId}/subtasks") public ApiResponse<BoardView> createSubtask(@PathVariable Long teamId,@PathVariable Long taskId,@Valid @RequestBody SubtaskInput r,Authentication a){return ok(service.saveSubtask(teamId,taskId,null,r,a.getName()));}
    @PutMapping("/tasks/{taskId}/subtasks/{id}") public ApiResponse<BoardView> updateSubtask(@PathVariable Long teamId,@PathVariable Long taskId,@PathVariable Long id,@Valid @RequestBody SubtaskInput r,Authentication a){return ok(service.saveSubtask(teamId,taskId,id,r,a.getName()));}
    @DeleteMapping("/tasks/{taskId}/subtasks/{id}") public ApiResponse<BoardView> deleteSubtask(@PathVariable Long teamId,@PathVariable Long taskId,@PathVariable Long id,@RequestParam Long revision,Authentication a){return ok(service.deleteSubtask(teamId,taskId,id,revision,a.getName()));}
}
