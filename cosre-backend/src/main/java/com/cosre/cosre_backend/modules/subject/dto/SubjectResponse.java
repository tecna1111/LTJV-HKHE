package com.cosre.cosre_backend.modules.subject.dto;

import com.cosre.cosre_backend.modules.subject.entity.Subject;
import java.time.LocalDateTime;

public record SubjectResponse(Long id, String code, String name, String description, int credits,
        boolean active, LocalDateTime createdAt, LocalDateTime updatedAt) {
    public static SubjectResponse from(Subject subject) {
        return new SubjectResponse(subject.getId(), subject.getCode(), subject.getName(), subject.getDescription(),
                subject.getCredits(), subject.isActive(), subject.getCreatedAt(), subject.getUpdatedAt());
    }
}
