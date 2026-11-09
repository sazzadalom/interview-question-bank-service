-- V2: Convert numeric upload-batch IDs and their contribution references to CHAR(36) UUIDs.
-- Assumptions:
--   * question_upload_batches.id is currently a numeric primary key.
--   * question_contributions.upload_batch_id is the matching numeric foreign key.
--   * The existing foreign key is named fk_contribution_batch.
--   * No other tables reference question_upload_batches.id.
-- Review against the actual V1 schema before applying this migration.

-- 1. Remove the existing foreign key so both columns can be migrated.
ALTER TABLE question_contributions
    DROP FOREIGN KEY fk_contribution_batch;

-- 2. Add replacement UUID columns.
ALTER TABLE question_upload_batches
    ADD COLUMN id_uuid CHAR(36) NULL;

ALTER TABLE question_contributions
    ADD COLUMN upload_batch_id_uuid CHAR(36) NULL;

-- 3. Assign a UUID to every existing batch and copy the matching UUID
--    to every contribution that references that batch.
UPDATE question_upload_batches
SET id_uuid = UUID()
WHERE id_uuid IS NULL;

UPDATE question_contributions qc
JOIN question_upload_batches qb
    ON qc.upload_batch_id = qb.id
SET qc.upload_batch_id_uuid = qb.id_uuid;

-- 4. The following NOT NULL alterations will fail if any row was not mapped,
--    preventing the migration from silently leaving a missing UUID reference.
ALTER TABLE question_upload_batches
    MODIFY COLUMN id_uuid CHAR(36) NOT NULL;

ALTER TABLE question_contributions
    MODIFY COLUMN upload_batch_id_uuid CHAR(36) NOT NULL;

-- 5. Replace the numeric batch primary key with the UUID column.
ALTER TABLE question_upload_batches
    DROP PRIMARY KEY,
    DROP COLUMN id,
    CHANGE COLUMN id_uuid id CHAR(36) NOT NULL,
    ADD PRIMARY KEY (id);

-- 6. Replace the numeric contribution reference with its UUID counterpart.
ALTER TABLE question_contributions
    DROP COLUMN upload_batch_id,
    CHANGE COLUMN upload_batch_id_uuid upload_batch_id CHAR(36) NOT NULL;

-- 7. Restore the foreign key using matching CHAR(36) columns.
ALTER TABLE question_contributions
    ADD CONSTRAINT fk_contribution_batch
    FOREIGN KEY (upload_batch_id)
    REFERENCES question_upload_batches (id);
