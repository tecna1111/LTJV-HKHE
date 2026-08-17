package com.cosre.cosre_backend.modules.team.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity @Table(name = "team_milestone_progress") @IdClass(TeamMilestoneProgressId.class)
public class TeamMilestoneProgress {
    @Id @Column(name="team_id") private Long teamId;
    @Id @Column(name="milestone_id") private Long milestoneId;
    @Column(name="completed_by", nullable=false) private Long completedBy;
    @Column(name="completed_at", nullable=false) private LocalDateTime completedAt;
    @PrePersist void create(){ completedAt = LocalDateTime.now(); }
    public Long getTeamId(){ return teamId; } public void setTeamId(Long v){ teamId=v; }
    public Long getMilestoneId(){ return milestoneId; } public void setMilestoneId(Long v){ milestoneId=v; }
    public Long getCompletedBy(){ return completedBy; } public void setCompletedBy(Long v){ completedBy=v; }
}
