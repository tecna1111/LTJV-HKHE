package com.cosre.cosre_backend.config;

import com.cosre.cosre_backend.common.constants.RoleEnum;
import com.cosre.cosre_backend.modules.account.entity.User;
import com.cosre.cosre_backend.modules.account.service.AccountService;
import com.cosre.cosre_backend.modules.classroom.dto.ClassroomRequest;
import com.cosre.cosre_backend.modules.classroom.entity.Classroom;
import com.cosre.cosre_backend.modules.classroom.service.ClassroomService;
import com.cosre.cosre_backend.modules.subject.dto.SubjectRequest;
import com.cosre.cosre_backend.modules.subject.entity.Subject;
import com.cosre.cosre_backend.modules.subject.repository.SubjectRepository;
import com.cosre.cosre_backend.modules.subject.service.SubjectService;
import com.cosre.cosre_backend.modules.syllabus.dto.SyllabusRequest;
import com.cosre.cosre_backend.modules.syllabus.service.SyllabusService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Khởi tạo dữ liệu nền tảng khi ứng dụng chạy lần đầu trên một database mới (H2 hoặc MySQL).
 * Chia làm 2 nhóm, bật/tắt độc lập qua application-*.properties:
 *  1) Tài khoản hệ thống (ADMIN/HEAD_DEPT/STAFF/LECTURER/STUDENT) — app.bootstrap-*.enabled
 *  2) Dữ liệu học vụ demo (Subject + Syllabus + Classroom + gán thành viên) — app.bootstrap-demo-academic.enabled
 * Idempotent: chạy lại nhiều lần (mỗi lần start app) không tạo trùng dữ liệu.
 */
@Component
public class DataInitializer implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final AccountService accountService;
    private final SubjectService subjectService;
    private final SubjectRepository subjectRepository;
    private final SyllabusService syllabusService;
    private final ClassroomService classroomService;

    @Value("${app.bootstrap-admin.enabled:false}")
    private boolean enabled;
    @Value("${app.bootstrap-admin.username:admin}")
    private String username;
    @Value("${app.bootstrap-admin.email:admin@cosre.local}")
    private String email;
    @Value("${app.bootstrap-admin.password:Admin@123}")
    private String password;

    @Value("${app.bootstrap-users.enabled:false}")
    private boolean bootstrapUsersEnabled;
    @Value("${app.bootstrap-users.head-dept.username:headdept}")
    private String headDeptUsername;
    @Value("${app.bootstrap-users.head-dept.email:headdept@cosre.local}")
    private String headDeptEmail;
    @Value("${app.bootstrap-users.head-dept.password:HeadDept@123}")
    private String headDeptPassword;
    @Value("${app.bootstrap-users.staff.username:staff}")
    private String staffUsername;
    @Value("${app.bootstrap-users.staff.email:staff@cosre.local}")
    private String staffEmail;
    @Value("${app.bootstrap-users.staff.password:Staff@123}")
    private String staffPassword;

    // Tài khoản Lecturer/Student mẫu - cần cho các test/luồng cần đủ 5 role (Team, Project, Checkpoint...).
    @Value("${app.bootstrap-users.lecturer.username:lecturer}")
    private String lecturerUsername;
    @Value("${app.bootstrap-users.lecturer.email:lecturer@cosre.local}")
    private String lecturerEmail;
    @Value("${app.bootstrap-users.lecturer.password:Lecturer@123}")
    private String lecturerPassword;
    @Value("${app.bootstrap-users.student1.username:student1}")
    private String student1Username;
    @Value("${app.bootstrap-users.student1.email:student1@cosre.local}")
    private String student1Email;
    @Value("${app.bootstrap-users.student1.password:Student@123}")
    private String student1Password;
    @Value("${app.bootstrap-users.student2.username:student2}")
    private String student2Username;
    @Value("${app.bootstrap-users.student2.email:student2@cosre.local}")
    private String student2Email;
    @Value("${app.bootstrap-users.student2.password:Student@123}")
    private String student2Password;

    // Dữ liệu học vụ demo (Subject + Syllabus + Classroom), tắt mặc định để không "bẩn" một DB MySQL dùng chung
    // trừ khi người chạy chủ động bật (vd: seed 1 lần cho môi trường demo/staging).
    @Value("${app.bootstrap-demo-academic.enabled:false}")
    private boolean bootstrapDemoAcademicEnabled;

    public DataInitializer(AccountService accountService, SubjectService subjectService,
            SubjectRepository subjectRepository, SyllabusService syllabusService,
            ClassroomService classroomService) {
        this.accountService = accountService;
        this.subjectService = subjectService;
        this.subjectRepository = subjectRepository;
        this.syllabusService = syllabusService;
        this.classroomService = classroomService;
    }

    @Override
    public void run(String... args) {
        if (enabled) {
            createUserIfMissing(username, email, password, "System Administrator", RoleEnum.ADMIN);
        }

        User lecturer = null;
        User student1 = null;
        User student2 = null;

        if (bootstrapUsersEnabled) {
            createUserIfMissing(headDeptUsername, headDeptEmail, headDeptPassword,
                    "Trưởng bộ môn COSRE", RoleEnum.HEAD_DEPT);
            createUserIfMissing(staffUsername, staffEmail, staffPassword,
                    "Nhân viên đào tạo COSRE", RoleEnum.STAFF);
            lecturer = createUserIfMissing(lecturerUsername, lecturerEmail, lecturerPassword,
                    "Giảng viên Demo COSRE", RoleEnum.LECTURER);
            student1 = createUserIfMissing(student1Username, student1Email, student1Password,
                    "Sinh viên Demo Một", RoleEnum.STUDENT);
            student2 = createUserIfMissing(student2Username, student2Email, student2Password,
                    "Sinh viên Demo Hai", RoleEnum.STUDENT);
        }

        if (bootstrapDemoAcademicEnabled) {
            seedDemoAcademicData(lecturer, student1, student2);
        }
    }

    /**
     * Tạo 1 Subject + 1 Syllabus + 1 Classroom mẫu, gán sẵn Lecturer + 2 Student demo.
     * Cho phép các module khác (Team/Project/Checkpoint...) có ngay dữ liệu để test tích hợp
     * mà không phải tự tạo lại từ đầu qua UI mỗi lần dựng môi trường mới.
     */
    private void seedDemoAcademicData(User lecturer, User student1, User student2) {
        if (subjectRepository.findByCodeIgnoreCase("SE101").isPresent()) {
            log.info("Demo academic data already present, skip seeding.");
            return;
        }
        if (lecturer == null || student1 == null || student2 == null) {
            log.warn("Skip demo academic data: app.bootstrap-users.enabled must be true to have "
                    + "a lecturer/student to attach the demo classroom to.");
            return;
        }

        Subject subject = subjectService.create(new SubjectRequest(
                "SE101",
                "Nhập môn Công nghệ phần mềm",
                "Môn học demo dùng để kiểm thử luồng Subject -> Syllabus -> Classroom -> Project -> Team.",
                3));

        syllabusService.create(new SyllabusRequest(
                subject.getId(),
                "Đề cương Nhập môn Công nghệ phần mềm",
                "Chương 1: Tổng quan quy trình phát triển phần mềm.\n"
                        + "Chương 2: Thu thập & phân tích yêu cầu.\n"
                        + "Chương 3: Thiết kế hệ thống.\n"
                        + "Chương 4: Kiểm thử & triển khai.",
                "Sinh viên nắm được vòng đời phát triển phần mềm và làm việc nhóm theo Agile.",
                "1.0"));

        Classroom classroom = classroomService.create(new ClassroomRequest(
                "SE101-DEMO", "SE101 - Lớp demo", subject.getId(), "HK1", "2025-2026"));

        classroomService.addMember(classroom.getId(), lecturer.getId(), RoleEnum.LECTURER);
        classroomService.addMember(classroom.getId(), student1.getId(), RoleEnum.STUDENT);
        classroomService.addMember(classroom.getId(), student2.getId(), RoleEnum.STUDENT);

        log.info("Seeded demo academic data: subject={}, classroom={}", subject.getCode(), classroom.getCode());
    }

    private User createUserIfMissing(String username, String email, String password, String fullName, RoleEnum role) {
        return accountService.findByUsername(username)
                .or(() -> accountService.findByEmail(email))
                .orElseGet(() -> accountService.createUser(username, email, password, fullName, role));
    }
}
