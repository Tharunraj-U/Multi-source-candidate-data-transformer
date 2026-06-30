ALTER TABLE processing_job
    ADD COLUMN last_attempt_at DATETIME(6) NULL AFTER started_at,
    ADD COLUMN attempt_count INT NOT NULL DEFAULT 0 AFTER last_attempt_at,
    ADD COLUMN max_attempts INT NOT NULL DEFAULT 3 AFTER attempt_count;