package com.cosre.cosre_backend.modules.authentication.service;

import com.cosre.cosre_backend.common.constants.RoleEnum;
import com.cosre.cosre_backend.config.JwtConfig;
import com.cosre.cosre_backend.modules.account.entity.User;
import com.cosre.cosre_backend.modules.account.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTests {
    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtConfig jwtConfig;
    private AuthService service;
    private User lecturer;

    @BeforeEach
    void setUp() {
        service = new AuthService(userRepository, passwordEncoder, jwtConfig);
        lecturer = new User();
        lecturer.setUsername("lecturer");
        lecturer.setPassword("encoded");
        lecturer.setFullName("Giảng viên Demo");
        lecturer.setRole(RoleEnum.LECTURER);
        lecturer.setActive(true);
    }

    @Test
    void loginReturnsTokenForValidCredentialsAndRole() {
        when(userRepository.findByUsername("lecturer")).thenReturn(Optional.of(lecturer));
        when(passwordEncoder.matches("secret", "encoded")).thenReturn(true);
        when(jwtConfig.generateToken("lecturer", RoleEnum.LECTURER)).thenReturn("jwt-token");
        when(jwtConfig.generateRefreshToken("lecturer", RoleEnum.LECTURER)).thenReturn("refresh-token");
        when(jwtConfig.getExpirationMs()).thenReturn(86400000L);

        var result = service.login("lecturer", "secret", RoleEnum.LECTURER);

        assertThat(result).isPresent();
        assertThat(result.orElseThrow().token()).isEqualTo("jwt-token");
        assertThat(result.orElseThrow().refreshToken()).isEqualTo("refresh-token");
        assertThat(result.orElseThrow().role()).isEqualTo("LECTURER");
    }

    @Test
    void loginRejectsWrongPassword() {
        when(userRepository.findByUsername("lecturer")).thenReturn(Optional.of(lecturer));
        when(passwordEncoder.matches("wrong", "encoded")).thenReturn(false);
        assertThat(service.login("lecturer", "wrong", RoleEnum.LECTURER)).isEmpty();
        verifyNoInteractions(jwtConfig);
    }

    @Test
    void loginRejectsInactiveAccount() {
        lecturer.setActive(false);
        when(userRepository.findByUsername("lecturer")).thenReturn(Optional.of(lecturer));
        assertThat(service.login("lecturer", "secret", RoleEnum.LECTURER)).isEmpty();
        verifyNoInteractions(passwordEncoder, jwtConfig);
    }

    @Test
    void loginRejectsSelectedRoleDifferentFromAccountRole() {
        when(userRepository.findByUsername("lecturer")).thenReturn(Optional.of(lecturer));
        assertThat(service.login("lecturer", "secret", RoleEnum.STUDENT)).isEmpty();
        verifyNoInteractions(passwordEncoder, jwtConfig);
    }

    @Test
    void refreshRotatesTokensForActiveAccount() {
        when(jwtConfig.validateToken("old-refresh-token")).thenReturn(true);
        when(jwtConfig.isRefreshToken("old-refresh-token")).thenReturn(true);
        when(jwtConfig.getUsername("old-refresh-token")).thenReturn("lecturer");
        when(jwtConfig.getRole("old-refresh-token")).thenReturn(RoleEnum.LECTURER);
        when(userRepository.findByUsername("lecturer")).thenReturn(Optional.of(lecturer));
        when(jwtConfig.generateToken("lecturer", RoleEnum.LECTURER)).thenReturn("new-access-token");
        when(jwtConfig.generateRefreshToken("lecturer", RoleEnum.LECTURER)).thenReturn("new-refresh-token");

        var result = service.refresh("old-refresh-token");

        assertThat(result).isPresent();
        assertThat(result.orElseThrow().token()).isEqualTo("new-access-token");
        assertThat(result.orElseThrow().refreshToken()).isEqualTo("new-refresh-token");
    }

    @Test
    void refreshRejectsAccessToken() {
        when(jwtConfig.validateToken("access-token")).thenReturn(true);
        when(jwtConfig.isRefreshToken("access-token")).thenReturn(false);

        assertThat(service.refresh("access-token")).isEmpty();
        verifyNoInteractions(userRepository);
    }
}
