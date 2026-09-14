CREATE TABLE team_milestone_progress (
    team_id BIGINT NOT NULL,
    milestone_id BIGINT NOT NULL,
    completed_by BIGINT NOT NULL,
    completed_at TIMESTAMP NOT NULL,
    CONSTRAINT pk_team_milestone_progress PRIMARY KEY (team_id, milestone_id),
    CONSTRAINT fk_progress_team FOREIGN KEY (team_id) REFERENCES teams(id),
    CONSTRAINT fk_progress_milestone FOREIGN KEY (milestone_id) REFERENCES project_milestones(id),
    CONSTRAINT fk_progress_user FOREIGN KEY (completed_by) REFERENCES users(id)
);
