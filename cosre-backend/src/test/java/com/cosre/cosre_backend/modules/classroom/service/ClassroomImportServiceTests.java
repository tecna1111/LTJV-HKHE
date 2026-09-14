package com.cosre.cosre_backend.modules.classroom.service;

import com.cosre.cosre_backend.common.constants.RoleEnum;
import com.cosre.cosre_backend.modules.account.entity.User;
import com.cosre.cosre_backend.modules.account.repository.UserRepository;
import com.cosre.cosre_backend.modules.classroom.entity.Classroom;
import com.cosre.cosre_backend.modules.classroom.repository.ClassroomRepository;
import com.cosre.cosre_backend.modules.subject.entity.Subject;
import com.cosre.cosre_backend.modules.subject.repository.SubjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClassroomImportServiceTests {

    @Mock private ClassroomRepository classroomRepository;
    @Mock private SubjectRepository subjectRepository;
    @Mock private UserRepository userRepository;
    private ClassroomImportService service;

    @BeforeEach
    void setUp() {
        service = new ClassroomImportService(classroomRepository, subjectRepository, userRepository);
    }

    @Test
    void importsValidClassroomRows() {
        when(subjectRepository.findByCodeIgnoreCase(anyString())).thenReturn(Optional.of(subject()));
        when(classroomRepository.findByCodeIgnoreCase(anyString())).thenReturn(Optional.empty());
        String csv = "code,name,subjectCode,semester,academicYear\n"
                + "SE1801,Software Engineering 01,SE101,Fall,2025-2026";

        var result = service.importClassrooms(csvFile(csv));

        assertThat(result.totalRows()).isEqualTo(1);
        assertThat(result.importedCount()).isOne();
        assertThat(result.failedCount()).isZero();
        verify(classroomRepository).save(any(Classroom.class));
    }

    @Test
    void reportsErrorWhenSubjectCodeDoesNotExist() {
        when(subjectRepository.findByCodeIgnoreCase(anyString())).thenReturn(Optional.empty());
        String csv = "code,name,subjectCode,semester,academicYear\n"
                + "SE1801,Software Engineering 01,UNKNOWN,Fall,2025-2026";

        var result = service.importClassrooms(csvFile(csv));

        assertThat(result.importedCount()).isZero();
        assertThat(result.failedCount()).isOne();
        assertThat(result.errors().get(0).message()).contains("Mã môn học không tồn tại");
    }

    @Test
    void reportsErrorWhenClassroomCodeAlreadyExists() {
        when(subjectRepository.findByCodeIgnoreCase(anyString())).thenReturn(Optional.of(subject()));
        when(classroomRepository.findByCodeIgnoreCase(anyString())).thenReturn(Optional.of(new Classroom()));
        String csv = "code,name,subjectCode,semester,academicYear\n"
                + "SE1801,Software Engineering 01,SE101,Fall,2025-2026";

        var result = service.importClassrooms(csvFile(csv));

        assertThat(result.failedCount()).isOne();
        assertThat(result.errors().get(0).message()).contains("đã tồn tại");
    }

    @Test
    void importsStudentsIntoExistingClassroom() {
        Classroom classroom = new Classroom();
        when(classroomRepository.findDetailedById(1L)).thenReturn(Optional.of(classroom));
        when(userRepository.findByUsername("sv001")).thenReturn(Optional.of(user(1L, RoleEnum.STUDENT)));
        String csv = "username\nsv001";

        var result = service.importMembers(1L, csvFile(csv), RoleEnum.STUDENT);

        assertThat(result.addedCount()).isOne();
        assertThat(result.failedCount()).isZero();
        assertThat(classroom.getStudents()).hasSize(1);
        verify(classroomRepository).save(classroom);
    }

    @Test
    void rejectsMemberImportWhenRoleDoesNotMatch() {
        Classroom classroom = new Classroom();
        when(classroomRepository.findDetailedById(1L)).thenReturn(Optional.of(classroom));
        when(userRepository.findByUsername("gv001")).thenReturn(Optional.of(user(2L, RoleEnum.LECTURER)));
        String csv = "username\ngv001";

        var result = service.importMembers(1L, csvFile(csv), RoleEnum.STUDENT);

        assertThat(result.addedCount()).isZero();
        assertThat(result.failedCount()).isOne();
        assertThat(result.errors().get(0).message()).contains("STUDENT");
    }

    @Test
    void skipsUserAlreadyAssignedToClassroom() {
        Classroom classroom = new Classroom();
        User existing = user(3L, RoleEnum.STUDENT);
        classroom.getStudents().add(existing);
        when(classroomRepository.findDetailedById(1L)).thenReturn(Optional.of(classroom));
        when(userRepository.findByUsername("sv003")).thenReturn(Optional.of(existing));
        String csv = "username\nsv003";

        var result = service.importMembers(1L, csvFile(csv), RoleEnum.STUDENT);

        assertThat(result.addedCount()).isZero();
        assertThat(result.failedCount()).isOne();
        assertThat(result.errors().get(0).message()).contains("đã có trong lớp học");
    }

    private Subject subject() {
        Subject subject = new Subject();
        subject.setCode("SE101");
        return subject;
    }

    private User user(Long id, RoleEnum role) {
        User user = new User();
        user.setId(id);
        user.setUsername("user" + id);
        user.setRole(role);
        return user;
    }

    private MockMultipartFile csvFile(String content) {
        return new MockMultipartFile("file", "classrooms.csv", "text/csv", content.getBytes(StandardCharsets.UTF_8));
    }
}
