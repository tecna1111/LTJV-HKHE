CREATE TABLE milestone_questions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    milestone_id BIGINT NOT NULL,
    question_text VARCHAR(1000) NOT NULL,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT pk_milestone_questions PRIMARY KEY (id),
    CONSTRAINT fk_milestone_question_milestone
        FOREIGN KEY (milestone_id)
        REFERENCES project_milestones(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_milestone_question_creator
        FOREIGN KEY (created_by)
        REFERENCES users(id)
);

CREATE TABLE milestone_answers (
    id BIGINT NOT NULL AUTO_INCREMENT,
    question_id BIGINT NOT NULL,
    team_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    answer_text VARCHAR(5000) NOT NULL,
    lecturer_feedback VARCHAR(2000),
    score DECIMAL(5,2),
    submitted_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    reviewed_at TIMESTAMP,
    reviewed_by BIGINT,
    CONSTRAINT pk_milestone_answers PRIMARY KEY (id),
    CONSTRAINT uk_milestone_answer
        UNIQUE (question_id, team_id, student_id),
    CONSTRAINT fk_milestone_answer_question
        FOREIGN KEY (question_id)
        REFERENCES milestone_questions(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_milestone_answer_team
        FOREIGN KEY (team_id)
        REFERENCES teams(id),
    CONSTRAINT fk_milestone_answer_student
        FOREIGN KEY (student_id)
        REFERENCES users(id),
    CONSTRAINT fk_milestone_answer_reviewer
        FOREIGN KEY (reviewed_by)
        REFERENCES users(id)
);

CREATE TABLE checkpoints (
    id BIGINT NOT NULL AUTO_INCREMENT,
    team_id BIGINT NOT NULL,
    milestone_id BIGINT,
    title VARCHAR(200) NOT NULL,
    description VARCHAR(2000),
    due_at TIMESTAMP NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP,
    CONSTRAINT pk_checkpoints PRIMARY KEY (id),
    CONSTRAINT fk_checkpoint_team
        FOREIGN KEY (team_id)
        REFERENCES teams(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_checkpoint_milestone
        FOREIGN KEY (milestone_id)
        REFERENCES project_milestones(id),
    CONSTRAINT fk_checkpoint_creator
        FOREIGN KEY (created_by)
        REFERENCES users(id)
);

CREATE TABLE checkpoint_assignments (
    checkpoint_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    assigned_at TIMESTAMP NOT NULL,
    CONSTRAINT pk_checkpoint_assignments
        PRIMARY KEY (checkpoint_id, student_id),
    CONSTRAINT fk_checkpoint_assignment_checkpoint
        FOREIGN KEY (checkpoint_id)
        REFERENCES checkpoints(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_checkpoint_assignment_student
        FOREIGN KEY (student_id)
        REFERENCES users(id)
);

CREATE TABLE checkpoint_submissions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    checkpoint_id BIGINT NOT NULL,
    submitted_by BIGINT NOT NULL,
    content VARCHAR(5000),
    attachment_url VARCHAR(1000),
    submitted_at TIMESTAMP NOT NULL,
    score DECIMAL(5,2),
    feedback VARCHAR(2000),
    reviewed_by BIGINT,
    reviewed_at TIMESTAMP,
    CONSTRAINT pk_checkpoint_submissions PRIMARY KEY (id),
    CONSTRAINT fk_submission_checkpoint
        FOREIGN KEY (checkpoint_id)
        REFERENCES checkpoints(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_submission_student
        FOREIGN KEY (submitted_by)
        REFERENCES users(id),
    CONSTRAINT fk_submission_reviewer
        FOREIGN KEY (reviewed_by)
        REFERENCES users(id)
);

CREATE INDEX idx_milestone_questions_milestone
    ON milestone_questions(milestone_id);

CREATE INDEX idx_milestone_answers_question
    ON milestone_answers(question_id);

CREATE INDEX idx_milestone_answers_team
    ON milestone_answers(team_id);

CREATE INDEX idx_checkpoints_team
    ON checkpoints(team_id);

CREATE INDEX idx_checkpoints_milestone
    ON checkpoints(milestone_id);

CREATE INDEX idx_checkpoint_submissions_checkpoint
    ON checkpoint_submissions(checkpoint_id);