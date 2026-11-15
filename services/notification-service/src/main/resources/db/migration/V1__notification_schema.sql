CREATE TABLE notification (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    recipient   VARCHAR(50)   NOT NULL,
    level       VARCHAR(10)   NOT NULL,
    category    VARCHAR(30)   NOT NULL,
    title       VARCHAR(200)  NOT NULL,
    message     VARCHAR(1000) NOT NULL,
    link        VARCHAR(500),
    data        JSON,
    source      VARCHAR(50)   NOT NULL,
    is_read     BOOLEAN       NOT NULL DEFAULT FALSE,
    read_at     DATETIME(6),
    created_at  DATETIME(6)   NOT NULL,
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE INDEX ix_notification_recipient ON notification (recipient, is_read, created_at);
