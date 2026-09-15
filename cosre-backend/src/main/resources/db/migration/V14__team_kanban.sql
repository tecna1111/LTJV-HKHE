CREATE TABLE kanban_boards (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 team_id BIGINT NOT NULL UNIQUE,
 revision BIGINT NOT NULL DEFAULT 0,
 CONSTRAINT fk_board_team FOREIGN KEY (team_id) REFERENCES teams(id)
);
CREATE TABLE kanban_sprints (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 board_id BIGINT NOT NULL,
 title VARCHAR(200) NOT NULL,
 starts_on DATE NOT NULL,
 ends_on DATE NOT NULL,
 CONSTRAINT fk_sprint_board FOREIGN KEY (board_id) REFERENCES kanban_boards(id)
);
CREATE TABLE kanban_tasks (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 board_id BIGINT NOT NULL,
 title VARCHAR(200) NOT NULL,
 description VARCHAR(4000),
 status VARCHAR(20) NOT NULL,
 weight DECIMAL(10,2) NOT NULL,
 due_on DATE,
 position INT NOT NULL,
 sprint_id BIGINT,
 milestone_id BIGINT,
 checkpoint_id BIGINT,
 created_by BIGINT NOT NULL,
 CONSTRAINT fk_task_board FOREIGN KEY (board_id) REFERENCES kanban_boards(id),
 CONSTRAINT fk_task_sprint FOREIGN KEY (sprint_id) REFERENCES kanban_sprints(id),
 CONSTRAINT fk_task_milestone FOREIGN KEY (milestone_id) REFERENCES project_milestones(id),
 CONSTRAINT fk_task_checkpoint FOREIGN KEY (checkpoint_id) REFERENCES checkpoints(id),
 CONSTRAINT fk_task_creator FOREIGN KEY (created_by) REFERENCES users(id),
 CONSTRAINT ck_task_weight CHECK (weight > 0),
 CONSTRAINT ck_task_status CHECK (status IN ('TODO','IN_PROGRESS','DONE'))
);
CREATE TABLE kanban_assignees (
 task_id BIGINT NOT NULL,
 user_id BIGINT NOT NULL,
 PRIMARY KEY (task_id, user_id),
 CONSTRAINT fk_assignee_task FOREIGN KEY (task_id) REFERENCES kanban_tasks(id) ON DELETE CASCADE,
 CONSTRAINT fk_assignee_user FOREIGN KEY (user_id) REFERENCES users(id)
);
CREATE TABLE kanban_subtasks (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 task_id BIGINT NOT NULL,
 title VARCHAR(200) NOT NULL,
 done BOOLEAN NOT NULL DEFAULT FALSE,
 CONSTRAINT fk_subtask_task FOREIGN KEY (task_id) REFERENCES kanban_tasks(id) ON DELETE CASCADE
);
CREATE TABLE kanban_events (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 board_id BIGINT NOT NULL,
 actor_id BIGINT NOT NULL,
 action VARCHAR(40) NOT NULL,
 detail VARCHAR(12000) NOT NULL,
 occurred_at TIMESTAMP NOT NULL,
 CONSTRAINT fk_event_board FOREIGN KEY (board_id) REFERENCES kanban_boards(id),
 CONSTRAINT fk_event_actor FOREIGN KEY (actor_id) REFERENCES users(id)
);
CREATE INDEX idx_task_board_order ON kanban_tasks(board_id, status, position);
CREATE INDEX idx_event_board ON kanban_events(board_id, id);
