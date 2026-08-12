package com.cosre.cosre_backend.modules.authentication.dto;

import com.cosre.cosre_backend.modules.account.entity.User;

public record CurrentUserResponse(Long id, String username, String email, String fullName, String role) {
    public static CurrentUserResponse from(User user) {
        return new CurrentUserResponse(user.getId(), user.getUsername(), user.getEmail(), user.getFullName(),
                user.getRole().name());
    }
}
