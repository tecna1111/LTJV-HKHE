package com.cosre.cosre_backend.modules.syllabus.entity;

import com.cosre.cosre_backend.modules.subject.entity.Subject;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "syllabi", uniqueConstraints = @UniqueConstraint(name = "uk_syllabi_subject_version", columnNames = {"subject_id", "version"}))
public class Syllabus {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;
    @Column(nullable = false) private String title;
    @Column(length = 5000) private String content;
    @Column(length = 4000) private String objectives;
    @Column(nullable = false, length = 30) private String version;
    @Column(nullable = false) private boolean isActive = true;
    @Column(nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(nullable = false) private LocalDateTime updatedAt;
    @PrePersist void create() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate void update() { updatedAt = LocalDateTime.now(); }
    public Long getId() { return id; }
    public Subject getSubject() { return subject; }
    public void setSubject(Subject subject) { this.subject = subject; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getObjectives() { return objectives; }
    public void setObjectives(String objectives) { this.objectives = objectives; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
