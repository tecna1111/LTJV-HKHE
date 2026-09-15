CREATE TABLE evaluation_rounds (
 id VARCHAR(64) NOT NULL PRIMARY KEY, final_open BOOLEAN NOT NULL DEFAULT FALSE, locked BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE TABLE final_evaluations (
 id VARCHAR(100) NOT NULL PRIMARY KEY, team_id BIGINT NOT NULL, project_id BIGINT NOT NULL,
 student_id BIGINT, lecturer_id BIGINT NOT NULL, score DECIMAL(5,2) NOT NULL,
 feedback VARCHAR(2000), updated_at TIMESTAMP NOT NULL,
 CONSTRAINT fk_final_team FOREIGN KEY (team_id) REFERENCES teams(id),
 CONSTRAINT fk_final_project FOREIGN KEY (project_id) REFERENCES projects(id),
 CONSTRAINT fk_final_student FOREIGN KEY (student_id) REFERENCES users(id),
 CONSTRAINT fk_final_lecturer FOREIGN KEY (lecturer_id) REFERENCES users(id),
 CONSTRAINT ck_final_score CHECK (score >= 0 AND score <= 10)
);
CREATE TABLE answer_peer_feedback (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, answer_id BIGINT NOT NULL, reviewer_id BIGINT NOT NULL,
 feedback VARCHAR(2000) NOT NULL, updated_at TIMESTAMP NOT NULL,
 CONSTRAINT uk_answer_reviewer UNIQUE (answer_id, reviewer_id),
 CONSTRAINT fk_feedback_answer FOREIGN KEY (answer_id) REFERENCES milestone_answers(id),
 CONSTRAINT fk_feedback_reviewer FOREIGN KEY (reviewer_id) REFERENCES users(id)
);
CREATE TABLE incident_reports (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, reporter_id BIGINT NOT NULL,
 title VARCHAR(200) NOT NULL, description VARCHAR(5000) NOT NULL, status VARCHAR(20) NOT NULL,
 resolution VARCHAR(2000), handled_by BIGINT, created_at TIMESTAMP NOT NULL, updated_at TIMESTAMP NOT NULL,
 version BIGINT NOT NULL DEFAULT 0,
 CONSTRAINT fk_incident_reporter FOREIGN KEY (reporter_id) REFERENCES users(id),
 CONSTRAINT fk_incident_handler FOREIGN KEY (handled_by) REFERENCES users(id)
);
ALTER TABLE resources ADD COLUMN milestone_id BIGINT;
ALTER TABLE resources ADD COLUMN checkpoint_id BIGINT;
ALTER TABLE resources ADD CONSTRAINT fk_resource_milestone FOREIGN KEY (milestone_id) REFERENCES project_milestones(id);
ALTER TABLE resources ADD CONSTRAINT fk_resource_checkpoint FOREIGN KEY (checkpoint_id) REFERENCES checkpoints(id);
CREATE INDEX idx_incident_reporter ON incident_reports(reporter_id, created_at);

-- Preserve legacy closed rounds, including prevention of new submissions after a lock.
INSERT INTO evaluation_rounds (id, final_open, locked)
SELECT CONCAT(team_id, ':', project_id),
       MAX(CASE WHEN milestone_id IS NULL THEN 1 ELSE 0 END),
       MAX(CASE WHEN status = 'LOCKED' THEN 1 ELSE 0 END)
FROM peer_evaluation GROUP BY team_id, project_id;
ALTER TABLE peer_evaluation DROP INDEX uk_peer_evaluation_unique;
ALTER TABLE peer_evaluation ADD CONSTRAINT uk_peer_evaluation_unique UNIQUE (evaluator_id, evaluatee_id, project_id, team_id, milestone_id);
