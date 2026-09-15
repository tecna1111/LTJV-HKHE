CREATE TABLE meetings (
    id BIGINT NOT NULL AUTO_INCREMENT,
    team_id BIGINT NOT NULL,
    organizer_id BIGINT NOT NULL,
    title VARCHAR(150) NOT NULL,
    description VARCHAR(1000),
    starts_at TIMESTAMP NOT NULL,
    ends_at TIMESTAMP NOT NULL,
    room_code VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL,
    reminder_sent_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT pk_meetings PRIMARY KEY (id),
    CONSTRAINT fk_meetings_team FOREIGN KEY (team_id) REFERENCES teams(id),
    CONSTRAINT fk_meetings_organizer FOREIGN KEY (organizer_id) REFERENCES users(id),
    CONSTRAINT uk_meetings_room_code UNIQUE (room_code)
);

CREATE INDEX idx_meetings_team_starts_at ON meetings(team_id, starts_at);
CREATE INDEX idx_meetings_reminder ON meetings(status, starts_at, reminder_sent_at);

ALTER TABLE notifications ADD COLUMN event_key VARCHAR(180) NULL;
CREATE UNIQUE INDEX uk_notifications_user_event_key ON notifications(user_id, event_key);
