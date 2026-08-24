package com.cosre.cosre_backend.modules.classroom.service;

import com.cosre.cosre_backend.common.constants.RoleEnum;
import com.cosre.cosre_backend.common.exception.DuplicateResourceException;
import com.cosre.cosre_backend.modules.account.entity.User;
import com.cosre.cosre_backend.modules.account.repository.UserRepository;
import com.cosre.cosre_backend.modules.classroom.entity.Classroom;
import com.cosre.cosre_backend.modules.classroom.repository.ClassroomRepository;
import com.cosre.cosre_backend.modules.subject.service.SubjectService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClassroomServiceTests {
    @Mock ClassroomRepository classroomRepository;
    @Mock UserRepository userRepository;
    @Mock SubjectService subjectService;
    @InjectMocks ClassroomService service;

    @Test
    void addMemberRejectsUserWithWrongRole() {
        Classroom classroom = new Classroom();
        User staff = user(7L, RoleEnum.STAFF);
        when(classroomRepository.findDetailedById(1L)).thenReturn(Optional.of(classroom));
        when(userRepository.findById(7L)).thenReturn(Optional.of(staff));

        assertThatThrownBy(() -> service.addMember(1L, 7L, RoleEnum.STUDENT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User must have role STUDENT");
        verify(classroomRepository, never()).save(any());
    }

    @Test
    void addMemberRejectsDuplicateAssignment() {
        Classroom classroom = new Classroom();
        User student = user(8L, RoleEnum.STUDENT);
        classroom.getStudents().add(student);
        when(classroomRepository.findDetailedById(1L)).thenReturn(Optional.of(classroom));
        when(userRepository.findById(8L)).thenReturn(Optional.of(student));

        assertThatThrownBy(() -> service.addMember(1L, 8L, RoleEnum.STUDENT))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("User is already assigned to this classroom");
    }

    private User user(Long id, RoleEnum role) {
        User user = new User(); user.setId(id); user.setRole(role); return user;
    }
}
