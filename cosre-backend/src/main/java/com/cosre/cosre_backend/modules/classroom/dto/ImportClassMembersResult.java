package com.cosre.cosre_backend.modules.classroom.dto;

import java.util.List;

public record ImportClassMembersResult(int totalRows, int addedCount, int failedCount,
        List<ImportClassMemberError> errors) {
}
