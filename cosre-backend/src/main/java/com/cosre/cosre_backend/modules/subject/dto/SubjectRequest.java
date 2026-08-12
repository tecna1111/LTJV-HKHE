package com.cosre.cosre_backend.modules.subject.dto;

import jakarta.validation.constraints.*;

/**
 * Payload request dùng để tạo hoặc cập nhật môn học.
 * Các annotation validation ngăn dữ liệu không hợp lệ hoặc thiếu thông tin đi tới tầng service.
 */
public record SubjectRequest(
        @NotBlank @Size(max = 30) String code,
        @NotBlank @Size(max = 255) String name,
        @Size(max = 2000) String description,
        @NotNull @Min(1) @Max(30) Integer credits) {
}
