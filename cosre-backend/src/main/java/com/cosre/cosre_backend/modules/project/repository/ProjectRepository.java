package com.cosre.cosre_backend.modules.project.repository;

import com.cosre.cosre_backend.modules.project.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    List<Project> findByCreatedByOrderByUpdatedAtDesc(Long createdBy);

    boolean existsBySubjectIdAndTitleIgnoreCase(
            Long subjectId,
            String title
    );

    boolean existsBySubjectIdAndTitleIgnoreCaseAndIdNot(
            Long subjectId,
            String title,
            Long id
    );
}
