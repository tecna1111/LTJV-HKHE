package com.cosre.cosre_backend.modules.syllabus.repository;

import com.cosre.cosre_backend.modules.syllabus.entity.Syllabus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SyllabusRepository extends JpaRepository<Syllabus, Long> {
    @EntityGraph(attributePaths = "subject") List<Syllabus> findBySubjectIdOrderByUpdatedAtDesc(Long subjectId);
    @EntityGraph(attributePaths = "subject") Optional<Syllabus> findDetailedById(Long id);
    boolean existsBySubjectIdAndVersionIgnoreCase(Long subjectId, String version);
}
