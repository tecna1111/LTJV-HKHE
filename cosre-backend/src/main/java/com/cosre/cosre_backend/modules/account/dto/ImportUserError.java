package com.cosre.cosre_backend.modules.account.dto;

public record ImportUserError(int row, String username, String message) {
}
