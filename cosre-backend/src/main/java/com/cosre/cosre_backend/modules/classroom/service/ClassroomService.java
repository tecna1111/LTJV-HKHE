package com.cosre.cosre_backend.modules.classroom.service;

import com.cosre.cosre_backend.common.constants.RoleEnum;
import com.cosre.cosre_backend.common.exception.*;
import com.cosre.cosre_backend.modules.account.entity.User;
import com.cosre.cosre_backend.modules.account.repository.UserRepository;
import com.cosre.cosre_backend.modules.classroom.dto.ClassroomRequest;
import com.cosre.cosre_backend.modules.classroom.entity.Classroom;
import com.cosre.cosre_backend.modules.classroom.repository.ClassroomRepository;
import com.cosre.cosre_backend.modules.subject.service.SubjectService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/**
 * Logic nghiệp vụ cho các thao tác liên quan đến lớp học.
 * Service này xử lý quy tắc truy cập, kiểm tra dữ liệu và quản lý thành viên của lớp học.
 */
@Service
@Transactional(readOnly = true)
public class ClassroomService {
    private final ClassroomRepository classroomRepository;
    private final UserRepository userRepository;
    private final SubjectService subjectService;

    public ClassroomService(ClassroomRepository classroomRepository, UserRepository userRepository, SubjectService subjectService) {
        this.classroomRepository = classroomRepository;
        this.userRepository = userRepository;
        this.subjectService = subjectService;
    }

    // Trả về các lớp học mà người dùng hiện tại có thể nhìn thấy.
    public List<Classroom> findAccessible(Authentication authentication) {
        if (hasAnyRole(authentication, "ADMIN", "STAFF", "HEAD_DEPT")) return classroomRepository.findAllDetailed();
        return classroomRepository.findAccessibleByUsername(authentication.getName());
    }

    // Kiểm tra xem người dùng hiện tại có quyền truy cập vào một lớp học cụ thể hay không.
    public Classroom getAccessible(Long id, Authentication authentication) {
        Classroom classroom = getDetailed(id);
        if (hasAnyRole(authentication, "ADMIN", "STAFF", "HEAD_DEPT") || isMember(classroom, authentication.getName())) return classroom;
        throw new AccessDeniedException("You cannot access this classroom");
    }

    // Lấy danh sách người dùng có thể được gán vào lớp học dựa trên vai trò.
    public List<User> findEligibleMembers(RoleEnum role) {
        if (role != RoleEnum.LECTURER && role != RoleEnum.STUDENT) {
            throw new IllegalArgumentException("Role must be LECTURER or STUDENT");
        }
        return userRepository.findByRoleAndIsActiveTrueOrderByFullName(role);
    }

    // Tạo một lớp học mới và lưu vào cơ sở dữ liệu.
    @Transactional
    public Classroom create(ClassroomRequest request) {
        ensureCodeAvailable(request.code(), null);
        Classroom classroom = new Classroom();
        apply(classroom, request);
        return classroomRepository.save(classroom);
    }

    // Cập nhật các giá trị của một lớp học hiện có.
    @Transactional
    public Classroom update(Long id, ClassroomRequest request) {
        Classroom classroom = getDetailed(id);
        ensureCodeAvailable(request.code(), id);
        apply(classroom, request);
        return classroomRepository.save(classroom);
    }

    // Bật hoặc tắt trạng thái hoạt động của lớp học.
    @Transactional
    public Classroom setActive(Long id, boolean active) {
        Classroom classroom = getDetailed(id);
        classroom.setActive(active);
        return classroomRepository.save(classroom);
    }

    // Thêm người dùng vào lớp học với vai trò mong muốn.
    @Transactional
    public Classroom addMember(Long classroomId, Long userId, RoleEnum expectedRole) {
        Classroom classroom = getDetailed(classroomId);
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getRole() != expectedRole) throw new IllegalArgumentException("User must have role " + expectedRole);
        boolean added = expectedRole == RoleEnum.LECTURER ? classroom.getLecturers().add(user) : classroom.getStudents().add(user);
        if (!added) throw new DuplicateResourceException("User is already assigned to this classroom");
        return classroomRepository.save(classroom);
    }

    // Xóa thành viên khỏi lớp học theo vai trò.
    @Transactional
    public Classroom removeMember(Long classroomId, Long userId, RoleEnum role) {
        Classroom classroom = getDetailed(classroomId);
        boolean removed = role == RoleEnum.LECTURER
                ? classroom.getLecturers().removeIf(user -> user.getId().equals(userId))
                : classroom.getStudents().removeIf(user -> user.getId().equals(userId));
        if (!removed) throw new ResourceNotFoundException("Class member not found");
        return classroomRepository.save(classroom);
    }

    // Tải lớp học kèm các quan hệ chi tiết để thực hiện thao tác sửa đổi.
    private Classroom getDetailed(Long id) {
        return classroomRepository.findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Classroom not found"));
    }

    // Đảm bảo mã lớp học là duy nhất trước khi tạo hoặc cập nhật.
    private void ensureCodeAvailable(String code, Long currentId) {
        classroomRepository.findByCodeIgnoreCase(code.trim()).ifPresent(existing -> {
            if (currentId == null || !currentId.equals(existing.getId())) throw new DuplicateResourceException("Classroom code already exists");
        });
    }

    // Sao chép dữ liệu từ request vào entity trước khi lưu.
    private void apply(Classroom classroom, ClassroomRequest request) {
        classroom.setCode(request.code().trim().toUpperCase());
        classroom.setName(request.name().trim());
        classroom.setSubject(subjectService.getById(request.subjectId()));
        classroom.setSemester(request.semester().trim());
        classroom.setAcademicYear(request.academicYear().trim());
    }

    // Kiểm tra xem người dùng đã là giảng viên hay sinh viên của lớp học này chưa.
    private boolean isMember(Classroom classroom, String username) {
        return classroom.getLecturers().stream().anyMatch(user -> user.getUsername().equals(username))
                || classroom.getStudents().stream().anyMatch(user -> user.getUsername().equals(username));
    }

    // Kiểm tra xem principal hiện tại có bất kỳ vai trò nào trong danh sách đã cho hay không.
    private boolean hasAnyRole(Authentication authentication, String... roles) {
        for (String role : roles) if (authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_" + role))) return true;
        return false;
    }
}
