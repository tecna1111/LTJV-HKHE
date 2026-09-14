package com.cosre.cosre_backend.modules.resource.service;

import com.cosre.cosre_backend.common.constants.RoleEnum;
import com.cosre.cosre_backend.common.exception.BusinessRuleException;
import com.cosre.cosre_backend.modules.account.entity.User;
import com.cosre.cosre_backend.modules.account.repository.UserRepository;
import com.cosre.cosre_backend.modules.classroom.entity.Classroom;
import com.cosre.cosre_backend.modules.classroom.repository.ClassroomRepository;
import com.cosre.cosre_backend.modules.resource.repository.ResourceRepository;
import com.cosre.cosre_backend.modules.team.repository.TeamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;

import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResourceServiceTests {
    @Mock ResourceRepository resourceRepository;
    @Mock ClassroomRepository classroomRepository;
    @Mock TeamRepository teamRepository;
    @Mock UserRepository userRepository;
    @TempDir Path storage;
    private ResourceService service;

    @BeforeEach
    void setUp() {
        service = new ResourceService(resourceRepository, classroomRepository, teamRepository,
                userRepository, storage.toString(), 10);
    }

    @Test
    void uploadRejectsFileLargerThanConfiguredLimit() {
        User staff = user(1L, "staff", RoleEnum.STAFF);
        when(classroomRepository.existsById(2L)).thenReturn(true);
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(staff));
        var file = new MockMultipartFile("file", "large.pdf", "application/pdf", new byte[11]);

        assertThatThrownBy(() -> service.uploadClassMaterial(2L, null, null, file, "staff"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("maximum allowed size");
        verify(resourceRepository, never()).save(any());
    }

    @Test
    void studentCannotListResourcesOfUnassignedClassroom() {
        User student = user(3L, "student", RoleEnum.STUDENT);
        when(userRepository.findByUsername("student")).thenReturn(Optional.of(student));
        when(classroomRepository.findDetailedById(9L)).thenReturn(Optional.of(new Classroom()));

        assertThatThrownBy(() -> service.listByClassroom(9L, "student"))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(resourceRepository);
    }

    private User user(Long id, String username, RoleEnum role) {
        User value = new User(); value.setId(id); value.setUsername(username); value.setRole(role); value.setActive(true);
        return value;
    }
}
