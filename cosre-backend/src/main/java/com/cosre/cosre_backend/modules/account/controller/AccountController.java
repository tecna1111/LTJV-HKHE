package com.cosre.cosre_backend.modules.account.controller;

import com.cosre.cosre_backend.common.dto.ApiResponse;
import com.cosre.cosre_backend.modules.account.dto.CreateUserRequest;
import com.cosre.cosre_backend.modules.account.dto.UpdateUserRequest;
import com.cosre.cosre_backend.modules.account.dto.UserResponse;
import com.cosre.cosre_backend.modules.account.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers() {
        List<UserResponse> users = accountService.findAllUsers().stream().map(UserResponse::from).toList();
        return ResponseEntity.ok(new ApiResponse<>(true, "Users loaded", users));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> createUser(@Valid @RequestBody CreateUserRequest request) {
        var user = accountService.createUser(request.username(), request.email(), request.password(),
                request.fullName(), request.role());
        return ResponseEntity.status(201).body(new ApiResponse<>(true, "User created", UserResponse.from(user)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(@PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request) {
        var user = accountService.updateUser(id, request.fullName(), request.role(), request.active());
        return ResponseEntity.ok(new ApiResponse<>(true, "User updated", UserResponse.from(user)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
        accountService.deleteUser(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "User deleted", null));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> setActive(@PathVariable Long id, @RequestParam boolean active) {
        var user = accountService.setActiveStatus(id, active);
        return ResponseEntity.ok(new ApiResponse<>(true, "User status updated", UserResponse.from(user)));
    }
}
