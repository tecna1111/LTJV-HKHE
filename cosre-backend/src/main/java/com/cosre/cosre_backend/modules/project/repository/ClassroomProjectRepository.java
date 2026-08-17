package com.cosre.cosre_backend.modules.project.repository;

import com.cosre.cosre_backend.modules.project.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ClassroomProjectRepository extends JpaRepository<ClassroomProject, ClassroomProjectId> {
    List<ClassroomProject> findByClassroomIdOrderByAssignedAtDesc(Long classroomId);
    boolean existsByClassroomIdAndProjectId(Long classroomId, Long projectId);
}
