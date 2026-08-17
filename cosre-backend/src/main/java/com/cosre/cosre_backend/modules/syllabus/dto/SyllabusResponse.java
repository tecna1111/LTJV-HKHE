package com.cosre.cosre_backend.modules.syllabus.dto;

import com.cosre.cosre_backend.modules.syllabus.entity.Syllabus;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

public record SyllabusResponse(Long id, Long subjectId, String subjectCode, String subjectName, String title,
        String content, List<String> objectives, String version, boolean active, LocalDateTime updatedAt) {
    public static SyllabusResponse from(Syllabus value) {
        List<String> objectives = value.getObjectives() == null || value.getObjectives().isBlank() ? List.of()
                : Arrays.stream(value.getObjectives().split("\\r?\\n")).map(String::trim).filter(s -> !s.isBlank()).toList();
        return new SyllabusResponse(value.getId(), value.getSubject().getId(), value.getSubject().getCode(),
                value.getSubject().getName(), value.getTitle(), value.getContent(), objectives, value.getVersion(),
                value.isActive(), value.getUpdatedAt());
    }
}
