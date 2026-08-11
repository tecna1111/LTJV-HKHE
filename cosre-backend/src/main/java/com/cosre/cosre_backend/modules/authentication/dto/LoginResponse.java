package com.cosre.cosre_backend.modules.authentication.dto;

public record LoginResponse(String token, String username, String fullName, String role) {
}
