CREATE TABLE collaboration_documents (
    team_id BIGINT NOT NULL,
    kind VARCHAR(16) NOT NULL,
    revision BIGINT NOT NULL DEFAULT 0,
    content MEDIUMTEXT NOT NULL,
    PRIMARY KEY (team_id, kind),
    CONSTRAINT fk_collaboration_team FOREIGN KEY (team_id) REFERENCES teams(id)
);
CREATE TABLE collaboration_operations (
    team_id BIGINT NOT NULL,
    kind VARCHAR(16) NOT NULL,
    operation_id VARCHAR(80) NOT NULL,
    actor VARCHAR(100) NOT NULL,
    payload MEDIUMTEXT NOT NULL,
    revision BIGINT NOT NULL,
    PRIMARY KEY (team_id, kind, operation_id),
    CONSTRAINT fk_collaboration_operation_document FOREIGN KEY (team_id, kind)
        REFERENCES collaboration_documents(team_id, kind)
);
