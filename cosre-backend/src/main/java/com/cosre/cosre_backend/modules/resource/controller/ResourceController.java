package com.cosre.cosre_backend.modules.resource.controller;

import com.cosre.cosre_backend.common.dto.ApiResponse;
import com.cosre.cosre_backend.modules.resource.dto.ResourceResponse;
import com.cosre.cosre_backend.modules.resource.entity.ResourceFile;
import com.cosre.cosre_backend.modules.resource.service.ResourceService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Controller REST cho module Resource: tài liệu môn học (theo lớp học) và
 * file bài nộp (theo nhóm). Upload dùng multipart/form-data, download trả
 * file trực tiếp kèm header Content-Disposition để trình duyệt lưu đúng
 * tên file gốc.
 */
@RestController
@RequestMapping("/api/v1/resources")
public class ResourceController {
    private final ResourceService resourceService;

    public ResourceController(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    // Giảng viên/Nhân viên/Admin tải tài liệu môn học lên cho một lớp học.
    @PostMapping(value = "/classroom/{classroomId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','STAFF','LECTURER')")
    public ResponseEntity<ApiResponse<ResourceResponse>> uploadClassMaterial(
            @PathVariable Long classroomId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String description,
            Authentication auth) {
        ResourceFile resource = resourceService.uploadClassMaterial(classroomId, title, description, file, auth.getName());
        return ResponseEntity.status(201).body(ok("Resource uploaded", ResourceResponse.from(resource)));
    }

    // Sinh viên/Giảng viên tải file bài nộp lên cho một nhóm.
    @PostMapping(value = "/team/{teamId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('STUDENT','LECTURER')")
    public ResponseEntity<ApiResponse<ResourceResponse>> uploadTeamSubmission(
            @PathVariable Long teamId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String description,
            Authentication auth) {
        ResourceFile resource = resourceService.uploadTeamSubmission(teamId, title, description, file, auth.getName());
        return ResponseEntity.status(201).body(ok("Resource uploaded", ResourceResponse.from(resource)));
    }

    // Danh sách tài liệu của một lớp học.
    @GetMapping("/classroom/{classroomId}")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF','HEAD_DEPT','LECTURER','STUDENT')")
    public ApiResponse<List<ResourceResponse>> byClassroom(@PathVariable Long classroomId, Authentication auth) {
        return ok("Resources loaded", resourceService.listByClassroom(classroomId, auth.getName()).stream().map(ResourceResponse::from).toList());
    }

    // Danh sách file bài nộp của một nhóm.
    @GetMapping("/team/{teamId}")
    @PreAuthorize("hasAnyRole('LECTURER','STUDENT')")
    public ApiResponse<List<ResourceResponse>> byTeam(@PathVariable Long teamId, Authentication auth) {
        return ok("Resources loaded", resourceService.listByTeam(teamId, auth.getName()).stream().map(ResourceResponse::from).toList());
    }

    // Tải file về máy; Service xác minh người dùng thuộc lớp/nhóm sở hữu file.
    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id, Authentication auth) {
        ResourceService.FileDownload download = resourceService.loadForDownload(id, auth.getName());
        Resource fileResource = new FileSystemResource(download.path());
        String encodedName = UriUtils.encode(download.resource().getOriginalFileName(), StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedName)
                .contentType(resolveContentType(download.resource().getContentType()))
                .body(fileResource);
    }

    // Xóa resource (kiểm tra quyền sở hữu/role trong Service).
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF','LECTURER','STUDENT')")
    public ApiResponse<Void> delete(@PathVariable Long id, Authentication auth) {
        resourceService.delete(id, auth.getName());
        return ok("Resource deleted", null);
    }

    private MediaType resolveContentType(String contentType) {
        try {
            return contentType == null ? MediaType.APPLICATION_OCTET_STREAM : MediaType.parseMediaType(contentType);
        } catch (Exception e) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    private <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(true, message, data);
    }
}
