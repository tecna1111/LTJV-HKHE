package com.cosre.cosre_backend.modules.account.dto;

import com.cosre.cosre_backend.common.constants.RoleEnum;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @Size(min = 1, max = 100) String fullName,
        RoleEnum role,
        Boolean active) {
}
