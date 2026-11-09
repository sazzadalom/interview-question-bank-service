-- V3: Convert questions.id and question_contributions.question_id to CHAR(36)
-- so they match the UUID fields used by the Java entities.
--
-- Assumptions to verify before applying:
--   * questions.id is currently a numeric primary key.
--   * question_contributions.question_id is the matching numeric foreign key.
--   * question_contributions is the only table referencing questions.id.
--   * The database is MySQL 8.0+.
--
-- This migration preserves existing question/contribution relationships by
-- generating one UUID per question and copying that UUID to its contributions.

-- 1. Find and drop the FK from question_contributions.question_id to questions.id.
SET @question_fk_name = (
    SELECT CONSTRAINT_NAME
    FROM information_schema.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'question_contributions'
      AND COLUMN_NAME = 'question_id'
      AND REFERENCED_TABLE_NAME = 'questions'
      AND REFERENCED_COLUMN_NAME = 'id'
    LIMIT 1
);

SET @drop_question_fk_sql = IF(
    @question_fk_name IS NULL,
    'SELECT 1',
    CONCAT('ALTER TABLE question_contributions DROP FOREIGN KEY `',
           REPLACE(@question_fk_name, '`', '``'), '`')
);
PREPARE drop_question_fk_stmt FROM @drop_question_fk_sql;
EXECUTE drop_question_fk_stmt;
DEALLOCATE PREPARE drop_question_fk_stmt;

-- 2. Add temporary UUID columns.
ALTER TABLE questions
    ADD COLUMN id_uuid CHAR(36) NULL;

ALTER TABLE question_contributions
    ADD COLUMN question_id_uuid CHAR(36) NULL;

-- 3. Generate one UUID per question and map every contribution to it.
UPDATE questions
SET id_uuid = UUID()
WHERE id_uuid IS NULL;

UPDATE question_contributions qc
JOIN questions q
    ON qc.question_id = q.id
SET qc.question_id_uuid = q.id_uuid;

-- 4. Enforce NOT NULL. These statements intentionally fail if any old
--    contribution did not map to an existing question.
ALTER TABLE questions
    MODIFY COLUMN id_uuid CHAR(36) NOT NULL;

ALTER TABLE question_contributions
    MODIFY COLUMN question_id_uuid CHAR(36) NOT NULL;

-- 5. Replace the numeric question primary key with its UUID.
ALTER TABLE questions
    DROP PRIMARY KEY,
    DROP COLUMN id,
    CHANGE COLUMN id_uuid id CHAR(36) NOT NULL,
    ADD PRIMARY KEY (id);

-- 6. Replace the numeric contribution reference with the UUID reference.
ALTER TABLE question_contributions
    DROP COLUMN question_id,
    CHANGE COLUMN question_id_uuid question_id CHAR(36) NOT NULL;

-- 7. Restore the FK with matching CHAR(36) columns.
ALTER TABLE question_contributions
    ADD CONSTRAINT fk_contribution_question
    FOREIGN KEY (question_id)
    REFERENCES questions (id);
