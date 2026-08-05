package com.cosre.cosre_backend.modules.authentication.controller;

import com.cosre.cosre_backend.common.dto.ApiResponse;
import com.cosre.cosre_backend.modules.authentication.dto.LoginRequest;
import com.cosre.cosre_backend.modules.authentication.dto.LoginResponse;
import com.cosre.cosre_backend.modules.authentication.dto.CurrentUserResponse;
import com.cosre.cosre_backend.modules.authentication.service.AuthService;
import com.cosre.cosre_backend.modules.account.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final AccountService accountService;

    public AuthController(AuthService authService, AccountService accountService) {
        this.authService = authService;
        this.accountService = accountService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request.getUsername(), request.getPassword())
                .map(response -> ResponseEntity.ok(new ApiResponse<>(true, "Login successful", response)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ApiResponse<LoginResponse>(false, "Invalid username or password", null)));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<CurrentUserResponse>> currentUser(Authentication authentication) {
        return accountService.findByUsername(authentication.getName())
                .map(user -> ResponseEntity.ok(new ApiResponse<>(true, "Current user loaded",
                        CurrentUserResponse.from(user))))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ApiResponse<CurrentUserResponse>(false, "Authenticated user not found", null)));
    }
}
