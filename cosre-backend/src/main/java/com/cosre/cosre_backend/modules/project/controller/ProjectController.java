package com.cosre.cosre_backend.modules.project.controller;

import com.cosre.cosre_backend.common.dto.ApiResponse;
import com.cosre.cosre_backend.modules.project.dto.CreateProjectRequest;
import com.cosre.cosre_backend.modules.project.dto.ProjectResponse;
import com.cosre.cosre_backend.modules.project.dto.UpdateProjectRequest;
import com.cosre.cosre_backend.modules.project.dto.ReviewProjectRequest;
import com.cosre.cosre_backend.modules.project.service.ProjectService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping
    @PreAuthorize("hasRole('LECTURER')")
    public ResponseEntity<ApiResponse<ProjectResponse>> create(
            @Valid @RequestBody CreateProjectRequest request,
            Authentication authentication
    ) {
        ProjectResponse project = projectService.create(
                request,
                authentication.getName()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new ApiResponse<>(
                        true,
                        "Project created",
                        project
                ));
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('LECTURER')")
    public ApiResponse<List<ProjectResponse>> getMine(
            Authentication authentication
    ) {
        return new ApiResponse<>(
                true,
                "Projects loaded",
                projectService.getMine(authentication.getName())
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('LECTURER')")
    public ApiResponse<ProjectResponse> getById(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return new ApiResponse<>(true, "Project loaded",
                projectService.getById(id, authentication.getName()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('LECTURER')")
    public ApiResponse<ProjectResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProjectRequest request,
            Authentication authentication
    ) {
        return new ApiResponse<>(true, "Project updated",
                projectService.update(id, request, authentication.getName()));
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasRole('LECTURER')")
    public ApiResponse<ProjectResponse> submit(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return new ApiResponse<>(
                true,
                "Project submitted for approval",
                projectService.submit(
                        id,
                        authentication.getName()
                )
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('LECTURER')")
    public ApiResponse<Void> delete(
            @PathVariable Long id,
            Authentication authentication
    ) {
        projectService.delete(id, authentication.getName());
        return new ApiResponse<>(true, "Project deleted", null);
    }

    @GetMapping("/review")
    @PreAuthorize("hasRole('HEAD_DEPT')")
    public ApiResponse<List<ProjectResponse>> reviewQueue(@RequestParam(required = false) com.cosre.cosre_backend.modules.project.entity.ProjectStatus status) {
        return new ApiResponse<>(true, "Projects loaded", projectService.listForReview(status));
    }

    @GetMapping("/approved")
    @PreAuthorize("hasAnyRole('HEAD_DEPT','LECTURER','STAFF')")
    public ApiResponse<List<ProjectResponse>> approved() {
        return new ApiResponse<>(true, "Approved projects loaded", projectService.listForReview(com.cosre.cosre_backend.modules.project.entity.ProjectStatus.APPROVED));
    }

    @PutMapping("/{id}/review")
    @PreAuthorize("hasRole('HEAD_DEPT')")
    public ApiResponse<ProjectResponse> review(@PathVariable Long id, @Valid @RequestBody ReviewProjectRequest request, Authentication authentication) {
        return new ApiResponse<>(true, request.approved() ? "Project approved" : "Project denied",
                projectService.review(id, request.approved(), request.note(), authentication.getName()));
    }

    @PutMapping("/{id}/classrooms/{classroomId}")
    @PreAuthorize("hasAnyRole('HEAD_DEPT','LECTURER')")
    public ApiResponse<Void> assign(@PathVariable Long id, @PathVariable Long classroomId, Authentication authentication) {
        projectService.assignToClassroom(id, classroomId, authentication.getName());
        return new ApiResponse<>(true, "Project assigned to classroom", null);
    }

    @GetMapping("/classroom/{classroomId}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<ProjectResponse>> classroomProjects(@PathVariable Long classroomId, Authentication authentication) {
        return new ApiResponse<>(true, "Classroom projects loaded", projectService.listClassroomProjects(classroomId, authentication.getName()));
    }
}
