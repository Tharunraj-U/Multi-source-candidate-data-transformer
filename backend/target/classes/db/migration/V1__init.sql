CREATE TABLE candidate (
    candidate_id         CHAR(36)        NOT NULL,
    full_name            VARCHAR(255)    NULL,
    headline             VARCHAR(500)    NULL,
    years_experience     DECIMAL(4,1)    NULL,
    overall_confidence   DECIMAL(5,4)    NULL,
    profile_picture_path VARCHAR(1000)   NULL,
    resume_path          VARCHAR(1000)   NULL,
    location             VARCHAR(255)    NULL,
    status               VARCHAR(20)     NOT NULL DEFAULT 'DRAFT',
    is_deleted           TINYINT(1)      NOT NULL DEFAULT 0,
    created_at           DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at           DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (candidate_id),
    CONSTRAINT CK_candidate_status CHECK (status IN ('DRAFT','PROCESSING','COMPLETED','PARTIAL','FAILED'))
);

CREATE TABLE raw_source (
    source_id            CHAR(36)        NOT NULL,
    candidate_id         CHAR(36)        NOT NULL,
    source_type          VARCHAR(30)     NOT NULL,
    storage_path         VARCHAR(1000)   NULL,
    original_filename    VARCHAR(255)    NULL,
    source_url           VARCHAR(2000)   NULL,
    status               VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    error_code           VARCHAR(50)     NULL,
    error_message        TEXT            NULL,
    ingested_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    processed_at         DATETIME(6)     NULL,
    PRIMARY KEY (source_id),
    CONSTRAINT FK_raw_source_candidate FOREIGN KEY (candidate_id) REFERENCES candidate(candidate_id),
    CONSTRAINT CK_raw_source_type CHECK (source_type IN ('RESUME','LINKEDIN','GITHUB','RECRUITER_CSV','ATS_JSON')),
    CONSTRAINT CK_raw_source_status CHECK (status IN ('PENDING','PROCESSING','COMPLETED','FAILED'))
);

CREATE TABLE candidate_email (
    id                   BIGINT          NOT NULL AUTO_INCREMENT,
    candidate_id         CHAR(36)        NOT NULL,
    email_address        VARCHAR(255)    NOT NULL,
    is_primary           TINYINT(1)      NOT NULL DEFAULT 0,
    confidence           DECIMAL(5,4)    NULL,
    PRIMARY KEY (id),
    CONSTRAINT FK_candidate_email_candidate FOREIGN KEY (candidate_id) REFERENCES candidate(candidate_id),
    CONSTRAINT UQ_candidate_email UNIQUE (candidate_id, email_address)
);

CREATE TABLE candidate_phone (
    id                   BIGINT          NOT NULL AUTO_INCREMENT,
    candidate_id         CHAR(36)        NOT NULL,
    phone_e164           VARCHAR(20)     NOT NULL,
    is_primary           TINYINT(1)      NOT NULL DEFAULT 0,
    confidence           DECIMAL(5,4)    NULL,
    PRIMARY KEY (id),
    CONSTRAINT FK_candidate_phone_candidate FOREIGN KEY (candidate_id) REFERENCES candidate(candidate_id),
    CONSTRAINT UQ_candidate_phone UNIQUE (candidate_id, phone_e164)
);

CREATE TABLE candidate_skill (
    id                   BIGINT          NOT NULL AUTO_INCREMENT,
    candidate_id         CHAR(36)        NOT NULL,
    skill_name           VARCHAR(255)    NOT NULL,
    canonical_skill      VARCHAR(255)    NULL,
    confidence           DECIMAL(5,4)    NULL,
    source_type          VARCHAR(30)     NULL,
    PRIMARY KEY (id),
    CONSTRAINT FK_candidate_skill_candidate FOREIGN KEY (candidate_id) REFERENCES candidate(candidate_id),
    CONSTRAINT UQ_candidate_skill UNIQUE (candidate_id, canonical_skill)
);

CREATE TABLE candidate_experience (
    id                   BIGINT          NOT NULL AUTO_INCREMENT,
    candidate_id         CHAR(36)        NOT NULL,
    company              VARCHAR(255)    NOT NULL,
    title                VARCHAR(255)    NULL,
    start_date           DATE            NULL,
    end_date             DATE            NULL,
    is_current           TINYINT(1)      NOT NULL DEFAULT 0,
    description          TEXT            NULL,
    confidence           DECIMAL(5,4)    NULL,
    source_type          VARCHAR(30)     NULL,
    PRIMARY KEY (id),
    CONSTRAINT FK_candidate_experience_candidate FOREIGN KEY (candidate_id) REFERENCES candidate(candidate_id)
);

CREATE TABLE candidate_education (
    id                   BIGINT          NOT NULL AUTO_INCREMENT,
    candidate_id         CHAR(36)        NOT NULL,
    institution          VARCHAR(255)    NOT NULL,
    degree               VARCHAR(255)    NULL,
    field_of_study       VARCHAR(255)    NULL,
    start_date           DATE            NULL,
    end_date             DATE            NULL,
    confidence           DECIMAL(5,4)    NULL,
    source_type          VARCHAR(30)     NULL,
    PRIMARY KEY (id),
    CONSTRAINT FK_candidate_education_candidate FOREIGN KEY (candidate_id) REFERENCES candidate(candidate_id)
);

CREATE TABLE candidate_link (
    id                   BIGINT          NOT NULL AUTO_INCREMENT,
    candidate_id         CHAR(36)        NOT NULL,
    link_type            VARCHAR(30)     NOT NULL,
    url                  VARCHAR(2000)   NOT NULL,
    confidence           DECIMAL(5,4)    NULL,
    PRIMARY KEY (id),
    CONSTRAINT FK_candidate_link_candidate FOREIGN KEY (candidate_id) REFERENCES candidate(candidate_id)
);

CREATE UNIQUE INDEX UQ_candidate_link ON candidate_link (candidate_id, link_type, url(191));

CREATE TABLE candidate_provenance (
    id                   BIGINT          NOT NULL AUTO_INCREMENT,
    candidate_id         CHAR(36)        NOT NULL,
    field_path           VARCHAR(255)    NOT NULL,
    source_type          VARCHAR(30)     NOT NULL,
    source_id            CHAR(36)        NULL,
    source_value         TEXT            NULL,
    raw_value            TEXT            NULL,
    captured_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT FK_candidate_provenance_candidate FOREIGN KEY (candidate_id) REFERENCES candidate(candidate_id),
    CONSTRAINT FK_candidate_provenance_source FOREIGN KEY (source_id) REFERENCES raw_source(source_id)
);

CREATE TABLE candidate_confidence (
    id                   BIGINT          NOT NULL AUTO_INCREMENT,
    candidate_id         CHAR(36)        NOT NULL,
    field_path           VARCHAR(255)    NOT NULL,
    score                DECIMAL(5,4)    NOT NULL,
    source_type          VARCHAR(30)     NULL,
    PRIMARY KEY (id),
    CONSTRAINT FK_candidate_confidence_candidate FOREIGN KEY (candidate_id) REFERENCES candidate(candidate_id),
    CONSTRAINT UQ_candidate_confidence UNIQUE (candidate_id, field_path)
);

CREATE TABLE runtime_config (
    id                   BIGINT          NOT NULL AUTO_INCREMENT,
    candidate_id         CHAR(36)        NOT NULL,
    config_json          TEXT            NOT NULL,
    created_at           DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT FK_runtime_config_candidate FOREIGN KEY (candidate_id) REFERENCES candidate(candidate_id)
);

CREATE TABLE processing_job (
    job_id               CHAR(36)        NOT NULL,
    candidate_id         CHAR(36)        NOT NULL,
    status               VARCHAR(20)     NOT NULL DEFAULT 'QUEUED',
    started_at           DATETIME(6)     NULL,
    completed_at         DATETIME(6)     NULL,
    error_message        TEXT            NULL,
    PRIMARY KEY (job_id),
    CONSTRAINT FK_processing_job_candidate FOREIGN KEY (candidate_id) REFERENCES candidate(candidate_id),
    CONSTRAINT CK_processing_job_status CHECK (status IN ('QUEUED','RUNNING','COMPLETED','FAILED'))
);

CREATE TABLE skill_alias (
    id                   BIGINT          NOT NULL AUTO_INCREMENT,
    alias                VARCHAR(255)    NOT NULL,
    canonical_name       VARCHAR(255)    NOT NULL,
    updated_at           DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT UQ_skill_alias UNIQUE (alias)
);

CREATE INDEX IX_candidate_status_updated ON candidate(status, updated_at);
CREATE INDEX IX_candidate_full_name ON candidate(full_name);
CREATE INDEX IX_candidate_email_address ON candidate_email(email_address);
CREATE INDEX IX_candidate_experience_company ON candidate_experience(company);
CREATE INDEX IX_raw_source_candidate ON raw_source(candidate_id, source_type);
CREATE INDEX IX_candidate_provenance_field ON candidate_provenance(candidate_id, field_path);
CREATE INDEX IX_skill_alias_alias ON skill_alias(alias);

INSERT INTO skill_alias (alias, canonical_name) VALUES
    ('Node JS', 'Node.js'),
    ('NodeJS', 'Node.js'),
    ('JS', 'JavaScript'),
    ('Springboot', 'Spring Boot'),
    ('K8s', 'Kubernetes'),
    ('ReactJS', 'React'),
    ('Postgres', 'PostgreSQL');
