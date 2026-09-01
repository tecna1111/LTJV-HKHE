package com.cosre.cosre_backend.modules.authentication.dto;

public record LoginResponse(
        String token,
        String refreshToken,
        long expiresIn,
        String username,
        String fullName,
        String role) {
}
