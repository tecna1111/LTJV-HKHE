package com.cosre.cosre_backend.modules.classroom.dto;

import com.cosre.cosre_backend.modules.classroom.entity.Classroom;
import com.cosre.cosre_backend.modules.subject.dto.SubjectResponse;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

public record ClassroomResponse(Long id, String code, String name, SubjectResponse subject, String semester,
        String academicYear, boolean active, List<ClassMemberResponse> lecturers, List<ClassMemberResponse> students,
        LocalDateTime createdAt, LocalDateTime updatedAt) {
    public static ClassroomResponse from(Classroom classroom) {
        Comparator<ClassMemberResponse> byName = Comparator.comparing(ClassMemberResponse::fullName);
        return new ClassroomResponse(classroom.getId(), classroom.getCode(), classroom.getName(),
                SubjectResponse.from(classroom.getSubject()), classroom.getSemester(), classroom.getAcademicYear(),
                classroom.isActive(), classroom.getLecturers().stream().map(ClassMemberResponse::from).sorted(byName).toList(),
                classroom.getStudents().stream().map(ClassMemberResponse::from).sorted(byName).toList(),
                classroom.getCreatedAt(), classroom.getUpdatedAt());
    }
}
