package com.cosre.cosre_backend.modules.classroom.dto;

import java.util.List;

public record ImportClassroomsResult(int totalRows, int importedCount, int failedCount,
        List<ImportClassroomError> errors) {
}
