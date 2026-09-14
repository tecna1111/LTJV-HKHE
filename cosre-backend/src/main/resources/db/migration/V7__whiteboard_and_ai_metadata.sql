CREATE TABLE whiteboards (
    id BIGINT NOT NULL AUTO_INCREMENT,
    team_id BIGINT NOT NULL,
    canvas_data TEXT NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    updated_by BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT pk_whiteboards PRIMARY KEY (id),
    CONSTRAINT uk_whiteboards_team UNIQUE (team_id),
    CONSTRAINT fk_whiteboards_team
        FOREIGN KEY (team_id) REFERENCES teams(id),
    CONSTRAINT fk_whiteboards_user
        FOREIGN KEY (updated_by) REFERENCES users(id)
);
CREATE INDEX idx_whiteboards_team
ON whiteboards(team_id);
ALTER TABLE chat_history ADD COLUMN user_id BIGINT;
ALTER TABLE chat_history ADD COLUMN team_id BIGINT;
ALTER TABLE chat_history ADD COLUMN created_at TIMESTAMP;
ALTER TABLE chat_history
ADD CONSTRAINT fk_chat_history_user
FOREIGN KEY (user_id) REFERENCES users(id);
CREATE INDEX idx_chat_history_user
ON chat_history(user_id);