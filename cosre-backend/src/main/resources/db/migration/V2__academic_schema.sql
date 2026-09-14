CREATE TABLE subjects (
    id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(30) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(2000),
    credits INT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT pk_subjects PRIMARY KEY (id),
    CONSTRAINT uk_subjects_code UNIQUE (code)
);

CREATE TABLE classrooms (
    id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    subject_id BIGINT NOT NULL,
    semester VARCHAR(30) NOT NULL,
    academic_year VARCHAR(20) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT pk_classrooms PRIMARY KEY (id),
    CONSTRAINT uk_classrooms_code UNIQUE (code),
    CONSTRAINT fk_classrooms_subject FOREIGN KEY (subject_id) REFERENCES subjects(id)
);

CREATE TABLE class_lecturers (
    classroom_id BIGINT NOT NULL,
    lecturer_id BIGINT NOT NULL,
    CONSTRAINT pk_class_lecturers PRIMARY KEY (classroom_id, lecturer_id),
    CONSTRAINT fk_class_lecturers_classroom FOREIGN KEY (classroom_id) REFERENCES classrooms(id),
    CONSTRAINT fk_class_lecturers_user FOREIGN KEY (lecturer_id) REFERENCES users(id)
);

CREATE TABLE class_students (
    classroom_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    CONSTRAINT pk_class_students PRIMARY KEY (classroom_id, student_id),
    CONSTRAINT fk_class_students_classroom FOREIGN KEY (classroom_id) REFERENCES classrooms(id),
    CONSTRAINT fk_class_students_user FOREIGN KEY (student_id) REFERENCES users(id)
);

CREATE INDEX idx_classrooms_subject ON classrooms(subject_id);
CREATE INDEX idx_class_lecturers_user ON class_lecturers(lecturer_id);
CREATE INDEX idx_class_students_user ON class_students(student_id);
