package com.cosre.cosre_backend.modules.project.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity @Table(name = "classroom_projects")
@IdClass(ClassroomProjectId.class)
public class ClassroomProject {
    @Id @Column(name = "classroom_id") private Long classroomId;
    @Id @Column(name = "project_id") private Long projectId;
    @Column(name = "assigned_by", nullable = false) private Long assignedBy;
    @Column(name = "assigned_at", nullable = false) private LocalDateTime assignedAt;
    @PrePersist void create() { if (assignedAt == null) assignedAt = LocalDateTime.now(); }
    public Long getClassroomId() { return classroomId; }
    public void setClassroomId(Long classroomId) { this.classroomId = classroomId; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public Long getAssignedBy() { return assignedBy; }
    public void setAssignedBy(Long assignedBy) { this.assignedBy = assignedBy; }
    public LocalDateTime getAssignedAt() { return assignedAt; }
}
