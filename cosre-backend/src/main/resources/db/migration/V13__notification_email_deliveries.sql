CREATE TABLE notification_email_deliveries (
    id BIGINT NOT NULL AUTO_INCREMENT,
    notification_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    event_key VARCHAR(180) NOT NULL,
    status VARCHAR(20) NOT NULL,
    attempts INT NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMP NULL,
    last_error VARCHAR(500) NULL,
    sent_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT pk_notification_email_deliveries PRIMARY KEY (id),
    CONSTRAINT uk_notification_email_delivery_event UNIQUE (user_id, event_key),
    CONSTRAINT fk_notification_email_delivery_notification FOREIGN KEY (notification_id) REFERENCES notifications(id),
    CONSTRAINT fk_notification_email_delivery_user FOREIGN KEY (user_id) REFERENCES users(id)
);
CREATE INDEX idx_notification_email_delivery_retry ON notification_email_deliveries(status, next_attempt_at);
