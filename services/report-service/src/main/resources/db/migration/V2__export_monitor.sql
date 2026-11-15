-- Excel exports made from the frontend grids (CSDXLMNT monitor, CSSXLMNT summary). Ported from office-app-api.
-- user_id is the username of the requester (users are owned by identity-service).

CREATE TABLE export_monitor (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    reference_number VARCHAR(20)  NOT NULL,
    user_id          VARCHAR(50)  NOT NULL,
    function_id      VARCHAR(20)  NOT NULL,
    fetch_size       INT,
    requested_time   DATETIME(6)  NOT NULL,
    response_time    DATETIME(6),
    export_status    VARCHAR(20)  NOT NULL,
    file_name        VARCHAR(255),
    storage_key      VARCHAR(500),
    total_records    INT,
    PRIMARY KEY (id),
    CONSTRAINT uk_export_monitor_reference UNIQUE (reference_number)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE INDEX ix_export_monitor_requested ON export_monitor (requested_time);
CREATE INDEX ix_export_monitor_user ON export_monitor (user_id, requested_time);
