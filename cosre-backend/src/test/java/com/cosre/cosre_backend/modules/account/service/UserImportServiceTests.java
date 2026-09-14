package com.cosre.cosre_backend.modules.account.service;

import com.cosre.cosre_backend.common.constants.RoleEnum;
import com.cosre.cosre_backend.modules.account.entity.User;
import com.cosre.cosre_backend.modules.account.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserImportServiceTests {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    private UserImportService service;

    @BeforeEach
    void setUp() {
        service = new UserImportService(userRepository, passwordEncoder);
        when(userRepository.findByUsername(any())).thenReturn(Optional.empty());
        when(userRepository.findByEmail(any())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenReturn("encoded-password");
    }

    @Test
    void importsValidStudentAndLecturerRows() {
        String csv = "username,email,fullName,password,role\n"
                + "sv001,sv001@example.edu.vn,Nguyen Van An,Student@123,STUDENT\n"
                + "gv001,gv001@example.edu.vn,Tran Thi Binh,Lecturer@123,LECTURER";

        var result = service.importUsers(csvFile(csv));

        assertThat(result.totalRows()).isEqualTo(2);
        assertThat(result.importedCount()).isEqualTo(2);
        assertThat(result.failedCount()).isZero();
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        assertThat(captor.getAllValues()).extracting(User::getRole)
                .containsExactly(RoleEnum.STUDENT, RoleEnum.LECTURER);
    }

    @Test
    void reportsInvalidRoleAndContinuesImportingOtherRows() {
        String csv = "username,email,fullName,password,role\n"
                + "admin01,admin@example.edu.vn,System Admin,Password@123,ADMIN\n"
                + "sv001,sv001@example.edu.vn,Nguyen Van An,Student@123,STUDENT";

        var result = service.importUsers(csvFile(csv));

        assertThat(result.importedCount()).isOne();
        assertThat(result.failedCount()).isOne();
        assertThat(result.errors().get(0).row()).isEqualTo(2);
        assertThat(result.errors().get(0).message()).contains("STUDENT", "LECTURER");
    }

    @Test
    void acceptsExcelSeparatorDirectiveInCsvTemplate() {
        String csv = "sep=,\nusername,email,fullName,password,role\n"
                + "sv001,sv001@example.edu.vn,Nguyen Van An,Student@123,STUDENT";

        var result = service.importUsers(csvFile(csv));

        assertThat(result.importedCount()).isOne();
        assertThat(result.failedCount()).isZero();
    }

    private MockMultipartFile csvFile(String content) {
        return new MockMultipartFile("file", "users.csv", "text/csv",
                content.getBytes(StandardCharsets.UTF_8));
    }
}
