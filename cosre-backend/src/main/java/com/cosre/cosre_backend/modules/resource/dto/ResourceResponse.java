package com.cosre.cosre_backend.modules.resource.dto;

import com.cosre.cosre_backend.modules.account.entity.User;
import com.cosre.cosre_backend.modules.resource.entity.ResourceCategory;
import com.cosre.cosre_backend.modules.resource.entity.ResourceFile;
import java.time.LocalDateTime;

public record ResourceResponse(Long id, String title, String description, ResourceCategory category,
        Long classroomId, Long teamId, String originalFileName, long fileSize, String contentType,
        Uploader uploadedBy, LocalDateTime createdAt) {

    public record Uploader(Long id, String username, String fullName) {
        static Uploader from(User user) {
            return user == null ? null : new Uploader(user.getId(), user.getUsername(), user.getFullName());
        }
    }

    public static ResourceResponse from(ResourceFile resource) {
        return new ResourceResponse(resource.getId(), resource.getTitle(), resource.getDescription(),
                resource.getCategory(), resource.getClassroomId(), resource.getTeamId(),
                resource.getOriginalFileName(), resource.getFileSize(), resource.getContentType(),
                Uploader.from(resource.getUploadedBy()), resource.getCreatedAt());
    }
}
