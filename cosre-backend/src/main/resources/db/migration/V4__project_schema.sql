CREATE TABLE projects (
    id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL,
    description VARCHAR(2000),
    subject_id BIGINT NOT NULL,
    created_by BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT pk_projects PRIMARY KEY (id),
    CONSTRAINT fk_projects_subject FOREIGN KEY (subject_id) REFERENCES subjects(id),
    CONSTRAINT fk_projects_creator FOREIGN KEY (created_by) REFERENCES users(id)
);

CREATE TABLE project_objectives (
    project_id BIGINT NOT NULL,
    objective VARCHAR(500) NOT NULL,
    CONSTRAINT fk_project_objectives_project
        FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
);

CREATE TABLE project_milestones (
    id BIGINT NOT NULL AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    description VARCHAR(1000),
    due_offset_days INT NOT NULL,
    display_order INT NOT NULL,
    CONSTRAINT pk_project_milestones PRIMARY KEY (id),
    CONSTRAINT fk_project_milestones_project
        FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
);

CREATE INDEX idx_projects_creator ON projects(created_by);
CREATE INDEX idx_projects_subject ON projects(subject_id);
CREATE INDEX idx_project_objectives_project ON project_objectives(project_id);
CREATE INDEX idx_project_milestones_project ON project_milestones(project_id);
