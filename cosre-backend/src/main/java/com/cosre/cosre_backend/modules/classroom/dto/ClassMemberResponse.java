package com.cosre.cosre_backend.modules.classroom.dto;

import com.cosre.cosre_backend.modules.account.entity.User;

public record ClassMemberResponse(Long id, String username, String email, String fullName, String role, boolean active) {
    public static ClassMemberResponse from(User user) {
        return new ClassMemberResponse(user.getId(), user.getUsername(), user.getEmail(), user.getFullName(),
                user.getRole().name(), user.isActive());
    }
}
