package com.cosre.cosre_backend.modules.classroom.service;

import com.cosre.cosre_backend.common.constants.RoleEnum;
import com.cosre.cosre_backend.common.exception.ResourceNotFoundException;
import com.cosre.cosre_backend.common.util.SpreadsheetReader;
import com.cosre.cosre_backend.common.util.SpreadsheetRow;
import com.cosre.cosre_backend.modules.account.entity.User;
import com.cosre.cosre_backend.modules.account.repository.UserRepository;
import com.cosre.cosre_backend.modules.classroom.dto.ImportClassMemberError;
import com.cosre.cosre_backend.modules.classroom.dto.ImportClassMembersResult;
import com.cosre.cosre_backend.modules.classroom.dto.ImportClassroomError;
import com.cosre.cosre_backend.modules.classroom.dto.ImportClassroomsResult;
import com.cosre.cosre_backend.modules.classroom.entity.Classroom;
import com.cosre.cosre_backend.modules.classroom.repository.ClassroomRepository;
import com.cosre.cosre_backend.modules.subject.entity.Subject;
import com.cosre.cosre_backend.modules.subject.repository.SubjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Xử lý nghiệp vụ import danh sách lớp học và danh sách thành viên (giảng viên/sinh viên) của lớp
 * học từ file CSV/XLSX. Đọc file bằng {@link SpreadsheetReader} (Apache POI cho định dạng .xlsx).
 */
@Service
@Transactional(readOnly = true)
public class ClassroomImportService {

    private static final List<String> CLASSROOM_HEADERS =
            List.of("code", "name", "subjectcode", "semester", "academicyear");
    private static final List<String> MEMBER_HEADERS = List.of("username");
    private static final Pattern ACADEMIC_YEAR_PATTERN = Pattern.compile("\\d{4}(-\\d{4})?");

    private final ClassroomRepository classroomRepository;
    private final SubjectRepository subjectRepository;
    private final UserRepository userRepository;

    public ClassroomImportService(ClassroomRepository classroomRepository, SubjectRepository subjectRepository,
            UserRepository userRepository) {
        this.classroomRepository = classroomRepository;
        this.subjectRepository = subjectRepository;
        this.userRepository = userRepository;
    }

    // Import danh sách lớp học ("Lớp") mới từ file CSV/XLSX do Staff tải lên.
    @Transactional
    public ImportClassroomsResult importClassrooms(MultipartFile file) {
        List<SpreadsheetRow> rows = SpreadsheetReader.readRows(file, CLASSROOM_HEADERS);
        List<ImportClassroomError> errors = new ArrayList<>();
        Set<String> codesInFile = new HashSet<>();
        int imported = 0;

        for (SpreadsheetRow row : rows) {
            String code = row.get("code");
            String validationError = validateClassroomRow(row, codesInFile);
            if (validationError != null) {
                errors.add(new ImportClassroomError(row.rowNumber(), code, validationError));
                continue;
            }

            Subject subject = subjectRepository.findByCodeIgnoreCase(row.get("subjectcode")).orElseThrow();
            Classroom classroom = new Classroom();
            classroom.setCode(code.toUpperCase(Locale.ROOT));
            classroom.setName(row.get("name"));
            classroom.setSubject(subject);
            classroom.setSemester(row.get("semester"));
            classroom.setAcademicYear(row.get("academicyear"));
            classroomRepository.save(classroom);
            codesInFile.add(code.toLowerCase(Locale.ROOT));
            imported++;
        }

        return new ImportClassroomsResult(rows.size(), imported, errors.size(), List.copyOf(errors));
    }

    // Import danh sách thành viên (SV hoặc GV, tùy theo expectedRole) vào một lớp học có sẵn.
    @Transactional
    public ImportClassMembersResult importMembers(Long classroomId, MultipartFile file, RoleEnum expectedRole) {
        if (expectedRole != RoleEnum.LECTURER && expectedRole != RoleEnum.STUDENT) {
            throw new IllegalArgumentException("Role must be LECTURER or STUDENT");
        }
        Classroom classroom = classroomRepository.findDetailedById(classroomId)
                .orElseThrow(() -> new ResourceNotFoundException("Classroom not found"));

        List<SpreadsheetRow> rows = SpreadsheetReader.readRows(file, MEMBER_HEADERS);
        List<ImportClassMemberError> errors = new ArrayList<>();
        Set<String> usernamesInFile = new HashSet<>();
        int added = 0;

        for (SpreadsheetRow row : rows) {
            String username = row.get("username");
            String error = validateMemberRow(username, expectedRole, classroom, usernamesInFile);
            if (error != null) {
                errors.add(new ImportClassMemberError(row.rowNumber(), username, error));
                continue;
            }

            User user = userRepository.findByUsername(username).orElseThrow();
            if (expectedRole == RoleEnum.LECTURER) classroom.getLecturers().add(user);
            else classroom.getStudents().add(user);
            usernamesInFile.add(username.toLowerCase(Locale.ROOT));
            added++;
        }

        classroomRepository.save(classroom);
        return new ImportClassMembersResult(rows.size(), added, errors.size(), List.copyOf(errors));
    }

    private String validateClassroomRow(SpreadsheetRow row, Set<String> codesInFile) {
        String code = row.get("code");
        String name = row.get("name");
        String subjectCode = row.get("subjectcode");
        String semester = row.get("semester");
        String academicYear = row.get("academicyear");

        if (code.isBlank() || code.length() > 50) return "Mã lớp không hợp lệ";
        if (name.isBlank() || name.length() > 255) return "Tên lớp không hợp lệ";
        if (semester.isBlank() || semester.length() > 30) return "Học kỳ không hợp lệ";
        if (!ACADEMIC_YEAR_PATTERN.matcher(academicYear).matches()) return "Năm học phải theo định dạng YYYY hoặc YYYY-YYYY";
        if (subjectCode.isBlank()) return "Mã môn học không được để trống";
        if (subjectRepository.findByCodeIgnoreCase(subjectCode).isEmpty()) return "Mã môn học không tồn tại";

        String codeKey = code.toLowerCase(Locale.ROOT);
        if (codesInFile.contains(codeKey)) return "Mã lớp bị trùng trong tệp";
        if (classroomRepository.findByCodeIgnoreCase(code).isPresent()) return "Mã lớp đã tồn tại";
        return null;
    }

    private String validateMemberRow(String username, RoleEnum expectedRole, Classroom classroom, Set<String> usernamesInFile) {
        if (username.isBlank()) return "Username không được để trống";
        String usernameKey = username.toLowerCase(Locale.ROOT);
        if (usernamesInFile.contains(usernameKey)) return "Username bị trùng trong tệp";

        var userOptional = userRepository.findByUsername(username);
        if (userOptional.isEmpty()) return "Username không tồn tại";
        User user = userOptional.get();
        if (user.getRole() != expectedRole) return "Người dùng không có vai trò " + expectedRole;

        boolean alreadyMember = expectedRole == RoleEnum.LECTURER
                ? classroom.getLecturers().stream().anyMatch(existing -> existing.getId().equals(user.getId()))
                : classroom.getStudents().stream().anyMatch(existing -> existing.getId().equals(user.getId()));
        if (alreadyMember) return "Người dùng đã có trong lớp học";
        return null;
    }
}
