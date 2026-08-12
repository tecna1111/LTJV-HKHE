CREATE TABLE chat_history (
    id BIGINT NOT NULL AUTO_INCREMENT,
    prompt VARCHAR(2000) NOT NULL,
    answer VARCHAR(5000) NOT NULL,
    CONSTRAINT pk_chat_history PRIMARY KEY (id)
);

CREATE TABLE chat_messages (
    id BIGINT NOT NULL AUTO_INCREMENT,
    room_type VARCHAR(255) NOT NULL,
    room_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT pk_chat_messages PRIMARY KEY (id)
);

CREATE TABLE teams (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    classroom_id BIGINT NOT NULL,
    project_id BIGINT,
    lecturer_id BIGINT NOT NULL,
    leader_id BIGINT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT pk_teams PRIMARY KEY (id),
    CONSTRAINT uk_team_class_name UNIQUE (classroom_id, name),
    CONSTRAINT fk_teams_classroom FOREIGN KEY (classroom_id) REFERENCES classrooms(id),
    CONSTRAINT fk_teams_lecturer FOREIGN KEY (lecturer_id) REFERENCES users(id),
    CONSTRAINT fk_teams_leader FOREIGN KEY (leader_id) REFERENCES users(id)
);

CREATE TABLE team_members (
    team_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    CONSTRAINT pk_team_members PRIMARY KEY (team_id, student_id),
    CONSTRAINT fk_team_members_team FOREIGN KEY (team_id) REFERENCES teams(id),
    CONSTRAINT fk_team_members_student FOREIGN KEY (student_id) REFERENCES users(id)
);

CREATE TABLE evaluation_criteria (
    id BIGINT NOT NULL AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    title VARCHAR(150) NOT NULL,
    description VARCHAR(500),
    max_score DECIMAL(38,2) NOT NULL,
    weight DECIMAL(38,2) NOT NULL,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    CONSTRAINT pk_evaluation_criteria PRIMARY KEY (id)
);

CREATE TABLE peer_evaluation (
    id BIGINT NOT NULL AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    milestone_id BIGINT,
    team_id BIGINT NOT NULL,
    evaluator_id BIGINT NOT NULL,
    evaluatee_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    total_score DECIMAL(38,2),
    comment VARCHAR(1000),
    submitted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    CONSTRAINT pk_peer_evaluation PRIMARY KEY (id),
    CONSTRAINT uk_peer_evaluation_unique UNIQUE (evaluator_id, evaluatee_id, project_id, milestone_id)
);

CREATE TABLE peer_evaluation_detail (
    id BIGINT NOT NULL AUTO_INCREMENT,
    peer_evaluation_id BIGINT NOT NULL,
    criteria_id BIGINT NOT NULL,
    score DECIMAL(38,2) NOT NULL,
    comment VARCHAR(500),
    CONSTRAINT pk_peer_evaluation_detail PRIMARY KEY (id),
    CONSTRAINT fk_peer_detail_evaluation FOREIGN KEY (peer_evaluation_id) REFERENCES peer_evaluation(id),
    CONSTRAINT fk_peer_detail_criteria FOREIGN KEY (criteria_id) REFERENCES evaluation_criteria(id)
);

CREATE INDEX idx_chat_messages_room ON chat_messages(room_type, room_id);
CREATE INDEX idx_chat_messages_sender ON chat_messages(sender_id);
CREATE INDEX idx_teams_classroom ON teams(classroom_id);
CREATE INDEX idx_team_members_student ON team_members(student_id);
CREATE INDEX idx_evaluation_criteria_project ON evaluation_criteria(project_id);
CREATE INDEX idx_peer_evaluation_team_project ON peer_evaluation(team_id, project_id);
CREATE INDEX idx_peer_evaluation_evaluatee ON peer_evaluation(evaluatee_id);
