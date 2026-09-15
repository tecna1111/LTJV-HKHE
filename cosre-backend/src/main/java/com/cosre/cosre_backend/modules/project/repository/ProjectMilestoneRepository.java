package com.cosre.cosre_backend.modules.project.repository;
import com.cosre.cosre_backend.modules.project.entity.ProjectMilestone;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ProjectMilestoneRepository extends JpaRepository<ProjectMilestone, Long> {
    java.util.List<ProjectMilestone> findByProjectIdOrderByDisplayOrderAsc(Long projectId);
}
