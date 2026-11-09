
CREATE TABLE topics (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(150) NOT NULL,
    slug VARCHAR(160) NOT NULL,
    description VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    CONSTRAINT uk_topics_name UNIQUE (name),
    CONSTRAINT uk_topics_slug UNIQUE (slug)
);

CREATE TABLE questions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    topic_id BIGINT NOT NULL,
    question_text TEXT NOT NULL,
    normalized_hash CHAR(64) NOT NULL,
    difficulty VARCHAR(30),
    expected_answer TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),

    CONSTRAINT uk_question_topic_hash
        UNIQUE (topic_id, normalized_hash),

    CONSTRAINT fk_questions_topic
        FOREIGN KEY (topic_id) REFERENCES topics(id)
);

CREATE TABLE question_upload_batches (
    id BIGINT NOT NULL AUTO_INCREMENT,
    candidate_id CHAR(36) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    status VARCHAR(30) NOT NULL,
    total_rows INT NOT NULL DEFAULT 0,
    valid_rows INT NOT NULL DEFAULT 0,
    new_questions INT NOT NULL DEFAULT 0,
    duplicate_questions INT NOT NULL DEFAULT 0,
    invalid_rows INT NOT NULL DEFAULT 0,
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP NULL,

    PRIMARY KEY (id),
    INDEX idx_upload_candidate (candidate_id),
    INDEX idx_upload_status (status)
);

CREATE TABLE question_contributions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    question_id BIGINT NOT NULL,
    candidate_id CHAR(36) NOT NULL,
    upload_batch_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),

    CONSTRAINT fk_contribution_question
        FOREIGN KEY (question_id) REFERENCES questions(id),

    CONSTRAINT fk_contribution_batch
        FOREIGN KEY (upload_batch_id)
        REFERENCES question_upload_batches(id),

    INDEX idx_contribution_candidate (candidate_id),
    INDEX idx_contribution_question (question_id)
);
