package com.cosre.cosre_backend.modules.evaluation.controller;

import com.cosre.cosre_backend.common.dto.ApiResponse;
import com.cosre.cosre_backend.common.exception.ResourceNotFoundException;
import com.cosre.cosre_backend.modules.account.service.AccountService;
import com.cosre.cosre_backend.modules.evaluation.dto.request.CriteriaRequest;
import com.cosre.cosre_backend.modules.evaluation.dto.response.CriteriaResponse;
import com.cosre.cosre_backend.modules.evaluation.service.EvaluationCriteriaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/evaluations/criteria")
@RequiredArgsConstructor
public class EvaluationCriteriaController {

    private final EvaluationCriteriaService criteriaService;
    private final AccountService accountService;

    // Chỉ giảng viên mới được tạo/sửa/xóa tiêu chí đánh giá.
    // NOTE: điều chỉnh lại role literal ("LECTURER") cho khớp với RoleEnum thực tế của dự án.
    @PostMapping
    @PreAuthorize("hasRole('LECTURER')")
    public ApiResponse<CriteriaResponse> create(@Valid @RequestBody CriteriaRequest request, Authentication authentication) {
        Long lecturerId = currentUserId(authentication);
        return ApiResponse.success(criteriaService.create(request, lecturerId), "Tạo tiêu chí thành công");
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('LECTURER')")
    public ApiResponse<CriteriaResponse> update(@PathVariable Long id, @Valid @RequestBody CriteriaRequest request,
            Authentication authentication) {
        Long lecturerId = currentUserId(authentication);
        return ApiResponse.success(criteriaService.update(id, request, lecturerId), "Cập nhật tiêu chí thành công");
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('LECTURER')")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> delete(@PathVariable Long id, Authentication authentication) {
        Long lecturerId = currentUserId(authentication);
        criteriaService.delete(id, lecturerId);
        return ApiResponse.success(null, "Xóa tiêu chí thành công");
    }

    @GetMapping("/project/{projectId}")
    public ApiResponse<List<CriteriaResponse>> getByProject(@PathVariable Long projectId) {
        return ApiResponse.success(criteriaService.getByProject(projectId));
    }

    private Long currentUserId(Authentication authentication) {
        return accountService.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"))
                .getId();
    }
}
