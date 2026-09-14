package com.cosre.cosre_backend.modules.authentication.service;

import com.cosre.cosre_backend.config.JwtConfig;
import com.cosre.cosre_backend.common.constants.RoleEnum;
import com.cosre.cosre_backend.modules.account.entity.User;
import com.cosre.cosre_backend.modules.account.repository.UserRepository;
import com.cosre.cosre_backend.modules.authentication.dto.LoginResponse;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtConfig jwtConfig;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtConfig jwtConfig) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtConfig = jwtConfig;
    }

    public Optional<LoginResponse> login(String username, String password, RoleEnum selectedRole) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return Optional.empty();
        }

        User user = userOpt.get();
        if (!user.isActive()
                || user.getRole() != selectedRole
                || !passwordEncoder.matches(password, user.getPassword())) {
            return Optional.empty();
        }

        return Optional.of(createSession(user));
    }

    @Transactional(readOnly = true)
    public Optional<LoginResponse> refresh(String refreshToken) {
        if (!jwtConfig.validateToken(refreshToken) || !jwtConfig.isRefreshToken(refreshToken)) {
            return Optional.empty();
        }

        return userRepository.findByUsername(jwtConfig.getUsername(refreshToken))
                .filter(User::isActive)
                .filter(user -> user.getRole() == jwtConfig.getRole(refreshToken))
                .map(this::createSession);
    }

    private LoginResponse createSession(User user) {
        String accessToken = jwtConfig.generateToken(user.getUsername(), user.getRole());
        String refreshToken = jwtConfig.generateRefreshToken(user.getUsername(), user.getRole());
        return new LoginResponse(
                accessToken,
                refreshToken,
                jwtConfig.getExpirationMs(),
                user.getUsername(),
                user.getFullName(),
                user.getRole().name());
    }
}
