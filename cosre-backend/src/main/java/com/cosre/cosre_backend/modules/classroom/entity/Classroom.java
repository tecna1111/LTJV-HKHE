package com.cosre.cosre_backend.modules.classroom.entity;

import com.cosre.cosre_backend.modules.account.entity.User;
import com.cosre.cosre_backend.modules.subject.entity.Subject;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Đại diện cho một lớp học trong hệ thống học thuật.
 * Mỗi lớp học thuộc về một môn học và có thể có nhiều giảng viên và sinh viên.
 */
@Entity
@Table(name = "classrooms")
public class Classroom {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false)
    private String name;

    // Môn học được giảng dạy trong lớp học này.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @Column(nullable = false, length = 30)
    private String semester;

    @Column(nullable = false, length = 20)
    private String academicYear;

    @Column(nullable = false)
    private boolean isActive = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // Danh sách giảng viên được gán vào lớp học này.
    @ManyToMany
    @JoinTable(name = "class_lecturers", joinColumns = @JoinColumn(name = "classroom_id"),
            inverseJoinColumns = @JoinColumn(name = "lecturer_id"))
    private Set<User> lecturers = new LinkedHashSet<>();

    // Danh sách sinh viên được gán vào lớp học này.
    @ManyToMany
    @JoinTable(name = "class_students", joinColumns = @JoinColumn(name = "classroom_id"),
            inverseJoinColumns = @JoinColumn(name = "student_id"))
    private Set<User> students = new LinkedHashSet<>();

    // Tự động thiết lập timestamp khi tạo mới hoặc cập nhật.
    @PrePersist
    void onCreate() { createdAt = LocalDateTime.now(); updatedAt = createdAt; }
    @PreUpdate
    void onUpdate() { updatedAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Subject getSubject() { return subject; }
    public void setSubject(Subject subject) { this.subject = subject; }
    public String getSemester() { return semester; }
    public void setSemester(String semester) { this.semester = semester; }
    public String getAcademicYear() { return academicYear; }
    public void setAcademicYear(String academicYear) { this.academicYear = academicYear; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public Set<User> getLecturers() { return lecturers; }
    public Set<User> getStudents() { return students; }
}
