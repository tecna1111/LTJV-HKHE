package com.cosre.cosre_backend.modules.account.dto;

import java.util.List;

public record ImportUsersResult(int totalRows, int importedCount, int failedCount,
                                List<ImportUserError> errors) {
}
