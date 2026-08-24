package com.cosre.cosre_backend.modules.classroom.controller;

import com.cosre.cosre_backend.common.constants.RoleEnum;
import com.cosre.cosre_backend.common.dto.ApiResponse;
import com.cosre.cosre_backend.modules.classroom.dto.*;
import com.cosre.cosre_backend.modules.classroom.service.ClassroomService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * Controller REST cho việc quản lý lớp học.
 * Tầng này nhận các request HTTP và chuyển việc xử lý sang tầng service.
 */
@RestController
@RequestMapping("/api/v1/classrooms")
public class ClassroomController {
    private final ClassroomService classroomService;

    public ClassroomController(ClassroomService classroomService) {
        this.classroomService = classroomService;
    }

    // Trả về danh sách người dùng đủ điều kiện được gán vào lớp học với vai trò giảng viên hoặc sinh viên.
    @GetMapping("/eligible-members")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<ApiResponse<List<ClassMemberResponse>>> eligibleMembers(@RequestParam RoleEnum role) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Eligible members loaded",
                classroomService.findEligibleMembers(role).stream().map(ClassMemberResponse::from).toList()));
    }

    // Trả về các lớp học mà người dùng hiện tại được phép xem.
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','STAFF','HEAD_DEPT','LECTURER','STUDENT')")
    public ResponseEntity<ApiResponse<List<ClassroomResponse>>> findAll(Authentication authentication) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Classrooms loaded",
                classroomService.findAccessible(authentication).stream().map(ClassroomResponse::from).toList()));
    }

    // Trả về thông tin chi tiết của một lớp học nếu người dùng hiện tại có quyền truy cập.
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF','HEAD_DEPT','LECTURER','STUDENT')")
    public ResponseEntity<ApiResponse<ClassroomResponse>> findOne(@PathVariable Long id, Authentication authentication) {
        return ok("Classroom loaded", classroomService.getAccessible(id, authentication));
    }

    // Tạo một lớp học mới từ payload được gửi lên.
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<ApiResponse<ClassroomResponse>> create(@Valid @RequestBody ClassroomRequest request) {
        return ResponseEntity.status(201).body(new ApiResponse<>(true, "Classroom created", ClassroomResponse.from(classroomService.create(request))));
    }

    // Cập nhật các thông tin cơ bản của một lớp học hiện có.
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<ApiResponse<ClassroomResponse>> update(@PathVariable Long id, @Valid @RequestBody ClassroomRequest request) {
        return ok("Classroom updated", classroomService.update(id, request));
    }

    // Kích hoạt hoặc vô hiệu hóa lớp học mà không xóa bản ghi.
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<ApiResponse<ClassroomResponse>> setStatus(@PathVariable Long id, @RequestParam boolean active) {
        return ok("Classroom status updated", classroomService.setActive(id, active));
    }

    // Gán một người dùng làm giảng viên cho lớp học.
    @PostMapping("/{id}/lecturers/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<ApiResponse<ClassroomResponse>> addLecturer(@PathVariable Long id, @PathVariable Long userId) {
        return ok("Lecturer assigned", classroomService.addMember(id, userId, RoleEnum.LECTURER));
    }

    // Xóa giảng viên khỏi lớp học.
    @DeleteMapping("/{id}/lecturers/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<ApiResponse<ClassroomResponse>> removeLecturer(@PathVariable Long id, @PathVariable Long userId) {
        return ok("Lecturer removed", classroomService.removeMember(id, userId, RoleEnum.LECTURER));
    }

    // Gán một người dùng làm sinh viên cho lớp học.
    @PostMapping("/{id}/students/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<ApiResponse<ClassroomResponse>> addStudent(@PathVariable Long id, @PathVariable Long userId) {
        return ok("Student assigned", classroomService.addMember(id, userId, RoleEnum.STUDENT));
    }

    // Xóa sinh viên khỏi lớp học.
    @DeleteMapping("/{id}/students/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<ApiResponse<ClassroomResponse>> removeStudent(@PathVariable Long id, @PathVariable Long userId) {
        return ok("Student removed", classroomService.removeMember(id, userId, RoleEnum.STUDENT));
    }

    // Phương thức trợ giúp để bọc entity lớp học thành format response API chung.
    private ResponseEntity<ApiResponse<ClassroomResponse>> ok(String message, com.cosre.cosre_backend.modules.classroom.entity.Classroom classroom) {
        return ResponseEntity.ok(new ApiResponse<>(true, message, ClassroomResponse.from(classroom)));
    }
}
