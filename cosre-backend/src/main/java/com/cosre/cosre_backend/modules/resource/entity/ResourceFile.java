package com.cosre.cosre_backend.modules.resource.entity;

import com.cosre.cosre_backend.modules.account.entity.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Đại diện cho một file được tải lên hệ thống: tài liệu môn học (do Giảng
 * viên/Nhân viên quản lý theo lớp học) hoặc file bài nộp (do Sinh viên nộp
 * theo nhóm). Bản thân file vật lý được lưu trên ổ đĩa server dưới tên
 * {@code storedFileName}; entity này chỉ giữ metadata.
 *
 * Lưu ý đặt tên: class được đặt là "ResourceFile" thay vì "Resource" để
 * tránh trùng tên với {@code org.springframework.core.io.Resource} (dùng
 * khi trả file cho client tải về ở Controller).
 */
@Entity
@Table(name = "resources")
public class ResourceFile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ResourceCategory category;

    // Bắt buộc khi category = CLASS_MATERIAL, null khi là bài nộp của nhóm.
    @Column(name = "classroom_id")
    private Long classroomId;

    // Bắt buộc khi category = TEAM_SUBMISSION, null khi là tài liệu lớp học.
    @Column(name = "team_id")
    private Long teamId;

    @Column(name = "original_file_name", nullable = false)
    private String originalFileName;

    @Column(name = "stored_file_name", nullable = false, unique = true)
    private String storedFileName;

    @Column(name = "content_type", length = 150)
    private String contentType;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploaded_by", nullable = false)
    private User uploadedBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public ResourceCategory getCategory() { return category; }
    public void setCategory(ResourceCategory category) { this.category = category; }
    public Long getClassroomId() { return classroomId; }
    public void setClassroomId(Long classroomId) { this.classroomId = classroomId; }
    public Long getTeamId() { return teamId; }
    public void setTeamId(Long teamId) { this.teamId = teamId; }
    public String getOriginalFileName() { return originalFileName; }
    public void setOriginalFileName(String originalFileName) { this.originalFileName = originalFileName; }
    public String getStoredFileName() { return storedFileName; }
    public void setStoredFileName(String storedFileName) { this.storedFileName = storedFileName; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }
    public User getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(User uploadedBy) { this.uploadedBy = uploadedBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
