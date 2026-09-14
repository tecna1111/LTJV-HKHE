package com.cosre.cosre_backend.modules.account.dto;

import com.cosre.cosre_backend.common.constants.RoleEnum;
import com.cosre.cosre_backend.modules.account.entity.User;

public record UserResponse(Long id, String username, String email, String fullName, RoleEnum role, boolean active) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getEmail(), user.getFullName(), user.getRole(),
                user.isActive());
    }
}
