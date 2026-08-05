package com.cosre.cosre_backend.modules.subject.service;

import com.cosre.cosre_backend.common.exception.DuplicateResourceException;
import com.cosre.cosre_backend.modules.subject.dto.SubjectRequest;
import com.cosre.cosre_backend.modules.subject.entity.Subject;
import com.cosre.cosre_backend.modules.subject.repository.SubjectRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubjectServiceTests {
    @Mock SubjectRepository repository;
    @InjectMocks SubjectService service;

    @Test
    void createNormalizesCodeAndPersistsSubject() {
        when(repository.findByCodeIgnoreCase("cos101")).thenReturn(Optional.empty());
        when(repository.save(any(Subject.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Subject result = service.create(new SubjectRequest(" cos101 ", "Nhập môn COSRE", "Cơ bản", 3));

        assertThat(result.getCode()).isEqualTo("COS101");
        assertThat(result.getName()).isEqualTo("Nhập môn COSRE");
        assertThat(result.getCredits()).isEqualTo(3);
        verify(repository).save(result);
    }

    @Test
    void createRejectsDuplicateCodeIgnoringCase() {
        Subject existing = new Subject();
        when(repository.findByCodeIgnoreCase("cos101")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.create(new SubjectRequest("cos101", "Tên", null, 3)))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Subject code already exists");
    }
}
