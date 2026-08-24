package com.cosre.cosre_backend.modules.team.entity;
import java.io.Serializable; import java.util.Objects;
public class TeamMilestoneProgressId implements Serializable {
    private Long teamId; private Long milestoneId; public TeamMilestoneProgressId(){} public TeamMilestoneProgressId(Long t,Long m){teamId=t;milestoneId=m;}
    @Override public boolean equals(Object o){return o instanceof TeamMilestoneProgressId v&&Objects.equals(teamId,v.teamId)&&Objects.equals(milestoneId,v.milestoneId);}
    @Override public int hashCode(){return Objects.hash(teamId,milestoneId);}
}
