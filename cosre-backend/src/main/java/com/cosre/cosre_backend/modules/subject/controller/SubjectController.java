package com.cosre.cosre_backend.modules.subject.controller;

import com.cosre.cosre_backend.common.dto.ApiResponse;
import com.cosre.cosre_backend.modules.subject.dto.*;
import com.cosre.cosre_backend.modules.subject.service.SubjectService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * Controller REST cho việc quản lý môn học.
 * Tầng này cung cấp các endpoint CRUD cho các môn học học thuật.
 */
@RestController
@RequestMapping("/api/v1/subjects")
public class SubjectController {
    private final SubjectService subjectService;

    public SubjectController(SubjectService subjectService) {
        this.subjectService = subjectService;
    }

    // Trả về tất cả môn học hoặc danh sách đã lọc khi client gửi từ khóa tìm kiếm.
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','STAFF','HEAD_DEPT','LECTURER','STUDENT')")
    public ResponseEntity<ApiResponse<List<SubjectResponse>>> findAll(@RequestParam(required = false) String query) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Subjects loaded",
                subjectService.findAll(query).stream().map(SubjectResponse::from).toList()));
    }

    // Trả về một môn học theo ID.
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF','HEAD_DEPT','LECTURER','STUDENT')")
    public ResponseEntity<ApiResponse<SubjectResponse>> findOne(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Subject loaded", SubjectResponse.from(subjectService.getById(id))));
    }

    // Tạo một môn học mới.
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<ApiResponse<SubjectResponse>> create(@Valid @RequestBody SubjectRequest request) {
        return ResponseEntity.status(201).body(new ApiResponse<>(true, "Subject created", SubjectResponse.from(subjectService.create(request))));
    }

    // Cập nhật một môn học hiện có.
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<ApiResponse<SubjectResponse>> update(@PathVariable Long id, @Valid @RequestBody SubjectRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Subject updated", SubjectResponse.from(subjectService.update(id, request))));
    }

    // Kích hoạt hoặc vô hiệu hóa một môn học mà không xóa khỏi hệ thống.
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<ApiResponse<SubjectResponse>> setStatus(@PathVariable Long id, @RequestParam boolean active) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Subject status updated", SubjectResponse.from(subjectService.setActive(id, active))));
    }
}
