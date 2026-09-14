package com.cosre.cosre_backend.modules.team.repository;
import com.cosre.cosre_backend.modules.team.entity.*; import org.springframework.data.jpa.repository.JpaRepository; import java.util.List;
public interface TeamMilestoneProgressRepository extends JpaRepository<TeamMilestoneProgress,TeamMilestoneProgressId>{ List<TeamMilestoneProgress> findByTeamId(Long teamId); }
