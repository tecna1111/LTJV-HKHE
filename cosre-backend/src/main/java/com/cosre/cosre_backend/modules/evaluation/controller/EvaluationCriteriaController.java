package com.cosre.cosre_backend.modules.evaluation.controller;

import com.cosre.cosre_backend.common.response.ApiResponse;
import com.cosre.cosre_backend.common.utils.SecurityUtils;
import com.cosre.cosre_backend.modules.evaluation.dto.request.CriteriaRequest;
import com.cosre.cosre_backend.modules.evaluation.dto.response.CriteriaResponse;
import com.cosre.cosre_backend.modules.evaluation.service.EvaluationCriteriaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/evaluations/criteria")
@RequiredArgsConstructor
public class EvaluationCriteriaController {

    private final EvaluationCriteriaService criteriaService;

    // Chỉ giảng viên mới được tạo/sửa/xóa tiêu chí đánh giá.
    // NOTE: điều chỉnh lại role literal ("LECTURER") cho khớp với RoleEnum thực tế của dự án.
    @PostMapping
    @PreAuthorize("hasRole('LECTURER')")
    public ApiResponse<CriteriaResponse> create(@Valid @RequestBody CriteriaRequest request) {
        Long lecturerId = SecurityUtils.getCurrentUserId();
        return ApiResponse.success(criteriaService.create(request, lecturerId), "Tạo tiêu chí thành công");
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('LECTURER')")
    public ApiResponse<CriteriaResponse> update(@PathVariable Long id, @Valid @RequestBody CriteriaRequest request) {
        Long lecturerId = SecurityUtils.getCurrentUserId();
        return ApiResponse.success(criteriaService.update(id, request, lecturerId), "Cập nhật tiêu chí thành công");
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('LECTURER')")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> delete(@PathVariable Long id) {
        Long lecturerId = SecurityUtils.getCurrentUserId();
        criteriaService.delete(id, lecturerId);
        return ApiResponse.success(null, "Xóa tiêu chí thành công");
    }

    @GetMapping("/project/{projectId}")
    public ApiResponse<List<CriteriaResponse>> getByProject(@PathVariable Long projectId) {
        return ApiResponse.success(criteriaService.getByProject(projectId));
    }
}
