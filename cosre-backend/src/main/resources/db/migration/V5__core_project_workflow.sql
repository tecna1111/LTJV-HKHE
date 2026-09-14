CREATE TABLE syllabi (
    id BIGINT NOT NULL AUTO_INCREMENT,
    subject_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    content VARCHAR(5000),
    objectives VARCHAR(4000),
    version VARCHAR(30) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT pk_syllabi PRIMARY KEY (id),
    CONSTRAINT uk_syllabi_subject_version UNIQUE (subject_id, version),
    CONSTRAINT fk_syllabi_subject FOREIGN KEY (subject_id) REFERENCES subjects(id)
);

ALTER TABLE projects ADD COLUMN syllabus_id BIGINT;
ALTER TABLE projects ADD COLUMN reviewed_by BIGINT;
ALTER TABLE projects ADD COLUMN review_note VARCHAR(1000);
ALTER TABLE projects ADD COLUMN reviewed_at TIMESTAMP;
ALTER TABLE projects ADD CONSTRAINT fk_projects_syllabus FOREIGN KEY (syllabus_id) REFERENCES syllabi(id);
ALTER TABLE projects ADD CONSTRAINT fk_projects_reviewer FOREIGN KEY (reviewed_by) REFERENCES users(id);

CREATE TABLE classroom_projects (
    classroom_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    assigned_by BIGINT NOT NULL,
    assigned_at TIMESTAMP NOT NULL,
    CONSTRAINT pk_classroom_projects PRIMARY KEY (classroom_id, project_id),
    CONSTRAINT fk_classroom_projects_classroom FOREIGN KEY (classroom_id) REFERENCES classrooms(id),
    CONSTRAINT fk_classroom_projects_project FOREIGN KEY (project_id) REFERENCES projects(id),
    CONSTRAINT fk_classroom_projects_assigner FOREIGN KEY (assigned_by) REFERENCES users(id)
);

CREATE INDEX idx_syllabi_subject ON syllabi(subject_id);
CREATE INDEX idx_classroom_projects_project ON classroom_projects(project_id);
