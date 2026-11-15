-- Report catalog, role permissions and execution history.
-- Roles and branches are owned by identity-service, so they are stored here by code (no cross-service FKs).

CREATE TABLE report_category (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    code        VARCHAR(30)  NOT NULL,
    name        VARCHAR(150) NOT NULL,
    sort_order  INT          NOT NULL DEFAULT 0,
    version     BIGINT       NOT NULL DEFAULT 0,
    created_at  DATETIME(6)  NOT NULL,
    created_by  VARCHAR(50),
    updated_at  DATETIME(6),
    updated_by  VARCHAR(50),
    PRIMARY KEY (id),
    CONSTRAINT uk_report_category_code UNIQUE (code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE report_definition (
    id                   BIGINT        NOT NULL AUTO_INCREMENT,
    code                 VARCHAR(50)   NOT NULL,
    title                VARCHAR(200)  NOT NULL,
    description          VARCHAR(1000),
    category_id          BIGINT,
    template_file_name   VARCHAR(255)  NOT NULL,
    template_storage_key VARCHAR(500)  NOT NULL,
    template_version     INT           NOT NULL DEFAULT 1,
    allowed_formats      VARCHAR(100)  NOT NULL,
    default_format       VARCHAR(10)   NOT NULL,
    execution_mode       VARCHAR(10)   NOT NULL,
    active               BOOLEAN       NOT NULL DEFAULT TRUE,
    version              BIGINT        NOT NULL DEFAULT 0,
    created_at           DATETIME(6)   NOT NULL,
    created_by           VARCHAR(50),
    updated_at           DATETIME(6),
    updated_by           VARCHAR(50),
    PRIMARY KEY (id),
    CONSTRAINT uk_report_definition_code UNIQUE (code),
    CONSTRAINT fk_report_definition_category FOREIGN KEY (category_id) REFERENCES report_category (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE report_parameter (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    report_id     BIGINT       NOT NULL,
    name          VARCHAR(100) NOT NULL,
    label         VARCHAR(200) NOT NULL,
    java_type     VARCHAR(100) NOT NULL,
    input_type    VARCHAR(30)  NOT NULL,
    required      BOOLEAN      NOT NULL DEFAULT FALSE,
    default_value VARCHAR(500),
    system_param  BOOLEAN      NOT NULL DEFAULT FALSE,
    sort_order    INT          NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_report_parameter UNIQUE (report_id, name),
    CONSTRAINT fk_report_parameter_report FOREIGN KEY (report_id) REFERENCES report_definition (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE report_permission (
    id             BIGINT      NOT NULL AUTO_INCREMENT,
    report_id      BIGINT      NOT NULL,
    role_code      VARCHAR(50) NOT NULL,
    branch_code    VARCHAR(10),
    execution_mode VARCHAR(10),
    PRIMARY KEY (id),
    CONSTRAINT fk_report_permission_report FOREIGN KEY (report_id) REFERENCES report_definition (id) ON DELETE CASCADE
) ENGINE = InnoDB;

CREATE INDEX ix_report_permission_role ON report_permission (role_code);

CREATE TABLE report_execution (
    id                 CHAR(36)      NOT NULL,
    report_id          BIGINT        NOT NULL,
    report_code        VARCHAR(50)   NOT NULL,
    report_title       VARCHAR(200)  NOT NULL,
    template_version   INT           NOT NULL,
    requested_by       VARCHAR(50)   NOT NULL,
    branch_code        VARCHAR(10),
    output_format      VARCHAR(10)   NOT NULL,
    execution_mode     VARCHAR(10)   NOT NULL,
    status             VARCHAR(20)   NOT NULL,
    parameters         JSON,
    output_storage_key VARCHAR(500),
    output_file_name   VARCHAR(255),
    output_size        BIGINT,
    page_count         INT,
    error_message      VARCHAR(1000),
    requested_at       DATETIME(6)   NOT NULL,
    started_at         DATETIME(6),
    finished_at        DATETIME(6),
    duration_ms        BIGINT,
    version            BIGINT        NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT fk_report_execution_report FOREIGN KEY (report_id) REFERENCES report_definition (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE INDEX ix_report_execution_user ON report_execution (requested_by, requested_at);
CREATE INDEX ix_report_execution_status ON report_execution (status, requested_at);

INSERT INTO report_category (code, name, sort_order, created_at, created_by) VALUES
    ('GENERAL',  'Ерөнхий',        10, NOW(6), 'system'),
    ('HR',       'Хүний нөөц',     20, NOW(6), 'system'),
    ('FINANCE',  'Санхүү',         30, NOW(6), 'system'),
    ('SECURITY', 'Хандалт, эрх',   40, NOW(6), 'system');
