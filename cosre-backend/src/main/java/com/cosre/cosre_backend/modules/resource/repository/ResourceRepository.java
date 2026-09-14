package com.cosre.cosre_backend.modules.resource.repository;

import com.cosre.cosre_backend.modules.resource.entity.ResourceFile;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ResourceRepository extends JpaRepository<ResourceFile, Long> {
    @EntityGraph(attributePaths = {"uploadedBy"})
    List<ResourceFile> findByClassroomIdOrderByCreatedAtDesc(Long classroomId);

    @EntityGraph(attributePaths = {"uploadedBy"})
    List<ResourceFile> findByTeamIdOrderByCreatedAtDesc(Long teamId);
}
