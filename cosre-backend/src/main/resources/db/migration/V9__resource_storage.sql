CREATE TABLE resources (
    id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    category VARCHAR(30) NOT NULL,
    classroom_id BIGINT,
    team_id BIGINT,
    original_file_name VARCHAR(255) NOT NULL,
    stored_file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(150),
    file_size BIGINT NOT NULL,
    uploaded_by BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT pk_resources PRIMARY KEY (id),
    CONSTRAINT uk_resources_stored_file UNIQUE (stored_file_name),
    CONSTRAINT fk_resources_classroom FOREIGN KEY (classroom_id) REFERENCES classrooms(id),
    CONSTRAINT fk_resources_team FOREIGN KEY (team_id) REFERENCES teams(id),
    CONSTRAINT fk_resources_uploader FOREIGN KEY (uploaded_by) REFERENCES users(id),
    CONSTRAINT ck_resources_scope CHECK (
        (category = 'CLASS_MATERIAL' AND classroom_id IS NOT NULL AND team_id IS NULL)
        OR (category = 'TEAM_SUBMISSION' AND team_id IS NOT NULL AND classroom_id IS NULL)
    )
);

CREATE INDEX idx_resources_classroom ON resources(classroom_id, created_at);
CREATE INDEX idx_resources_team ON resources(team_id, created_at);
CREATE INDEX idx_resources_uploader ON resources(uploaded_by);
