package com.cosre.cosre_backend.modules.team.controller;

import com.cosre.cosre_backend.common.dto.ApiResponse;
import com.cosre.cosre_backend.modules.team.dto.*;
import com.cosre.cosre_backend.modules.team.service.TeamService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/teams")
public class TeamController {
    private final TeamService service;
    public TeamController(TeamService service) { this.service = service; }

    @GetMapping
    @PreAuthorize("hasAnyRole('LECTURER','STUDENT')")
    public ApiResponse<List<TeamResponse>> mine(Authentication auth) {
        return ok("Teams loaded", service.listMine(auth.getName()).stream().map(TeamResponse::from).toList());
    }
    @GetMapping("/classroom/{classroomId}")
    @PreAuthorize("hasAnyRole('LECTURER','HEAD_DEPT','STAFF')")
    public ApiResponse<List<TeamResponse>> byClassroom(@PathVariable Long classroomId) {
        return ok("Teams loaded", service.listByClassroom(classroomId).stream().map(TeamResponse::from).toList());
    }
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('LECTURER','STUDENT')")
    public ApiResponse<TeamResponse> get(@PathVariable Long id, Authentication auth) { return ok("Team loaded", TeamResponse.from(service.get(id, auth.getName()))); }
    @GetMapping("/available-students")
    @PreAuthorize("hasRole('LECTURER')")
    public ApiResponse<List<TeamResponse.Person>> students(@RequestParam Long classroomId, Authentication auth) {
        return ok("Students loaded", service.availableStudents(classroomId, auth.getName()).stream().map(user -> new TeamResponse.Person(user.getId(), user.getUsername(), user.getFullName(), user.getEmail())).toList());
    }
    @PostMapping
    @PreAuthorize("hasRole('LECTURER')")
    public ResponseEntity<ApiResponse<TeamResponse>> create(@Valid @RequestBody CreateTeamRequest request, Authentication auth) {
        return ResponseEntity.status(201).body(ok("Team created", TeamResponse.from(service.create(request, auth.getName()))));
    }
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('LECTURER')")
    public ApiResponse<TeamResponse> update(@PathVariable Long id, @Valid @RequestBody UpdateTeamRequest request, Authentication auth) { return ok("Team updated", TeamResponse.from(service.update(id, request, auth.getName()))); }
    @PostMapping("/{id}/members")
    @PreAuthorize("hasRole('LECTURER')")
    public ApiResponse<TeamResponse> addMember(@PathVariable Long id, @Valid @RequestBody TeamMemberRequest request, Authentication auth) { return ok("Member added", TeamResponse.from(service.addMember(id, request.studentId(), request.leader(), auth.getName()))); }
    @DeleteMapping("/{id}/members/{studentId}")
    @PreAuthorize("hasRole('LECTURER')")
    public ApiResponse<TeamResponse> removeMember(@PathVariable Long id, @PathVariable Long studentId, Authentication auth) { return ok("Member removed", TeamResponse.from(service.removeMember(id, studentId, auth.getName()))); }
    @PutMapping("/{id}/project")
    @PreAuthorize("hasRole('LECTURER')")
    public ApiResponse<TeamResponse> project(@PathVariable Long id, @Valid @RequestBody AssignProjectRequest request, Authentication auth) { return ok("Project assigned", TeamResponse.from(service.assignProject(id, request.projectId(), auth.getName()))); }
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('LECTURER')")
    public ApiResponse<Void> delete(@PathVariable Long id, Authentication auth) { service.delete(id, auth.getName()); return ok("Team deleted", null); }
    @GetMapping("/{id}/workspace") @PreAuthorize("hasAnyRole('LECTURER','STUDENT')")
    public ApiResponse<TeamWorkspaceResponse> workspace(@PathVariable Long id, Authentication auth) { return ok("Workspace loaded", service.workspace(id, auth.getName())); }
    @PutMapping("/{id}/milestones/{milestoneId}") @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<TeamWorkspaceResponse> milestone(@PathVariable Long id, @PathVariable Long milestoneId, @RequestParam boolean done, Authentication auth) { return ok("Milestone updated", service.setMilestoneDone(id, milestoneId, done, auth.getName())); }
    private <T> ApiResponse<T> ok(String message, T data) { return new ApiResponse<>(true, message, data); }
}
