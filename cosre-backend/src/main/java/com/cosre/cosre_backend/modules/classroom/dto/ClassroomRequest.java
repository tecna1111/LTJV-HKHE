package com.cosre.cosre_backend.modules.classroom.dto;

import jakarta.validation.constraints.*;

/**
 * Payload request dùng để tạo hoặc cập nhật lớp học.
 * Các quy tắc validation đảm bảo client gửi dữ liệu đầy đủ và đúng định dạng.
 */
public record ClassroomRequest(
        @NotBlank @Size(max = 50) String code,
        @NotBlank @Size(max = 255) String name,
        @NotNull Long subjectId,
        @NotBlank @Size(max = 30) String semester,
        @NotBlank @Pattern(regexp = "\\d{4}(-\\d{4})?", message = "must be YYYY or YYYY-YYYY") String academicYear) {
}
