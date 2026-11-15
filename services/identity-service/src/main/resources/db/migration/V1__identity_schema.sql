-- Organization structure, employees and application users.
-- app_user shares its primary key with employee (app_user.employee_id = employee.id): every user IS an
-- employee, but an employee only becomes a user when an account is granted.

CREATE TABLE branch (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    code        VARCHAR(10)  NOT NULL,
    name        VARCHAR(150) NOT NULL,
    address     VARCHAR(500),
    phone       VARCHAR(32),
    parent_id   BIGINT,
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    version     BIGINT       NOT NULL DEFAULT 0,
    created_at  DATETIME(6)  NOT NULL,
    created_by  VARCHAR(50),
    updated_at  DATETIME(6),
    updated_by  VARCHAR(50),
    PRIMARY KEY (id),
    CONSTRAINT uk_branch_code UNIQUE (code),
    CONSTRAINT fk_branch_parent FOREIGN KEY (parent_id) REFERENCES branch (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE department (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    code        VARCHAR(20)  NOT NULL,
    name        VARCHAR(150) NOT NULL,
    branch_id   BIGINT,
    parent_id   BIGINT,
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    version     BIGINT       NOT NULL DEFAULT 0,
    created_at  DATETIME(6)  NOT NULL,
    created_by  VARCHAR(50),
    updated_at  DATETIME(6),
    updated_by  VARCHAR(50),
    PRIMARY KEY (id),
    CONSTRAINT uk_department_code UNIQUE (code),
    CONSTRAINT fk_department_branch FOREIGN KEY (branch_id) REFERENCES branch (id),
    CONSTRAINT fk_department_parent FOREIGN KEY (parent_id) REFERENCES department (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE job_position (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    code        VARCHAR(20)  NOT NULL,
    name        VARCHAR(150) NOT NULL,
    description VARCHAR(500),
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    version     BIGINT       NOT NULL DEFAULT 0,
    created_at  DATETIME(6)  NOT NULL,
    created_by  VARCHAR(50),
    updated_at  DATETIME(6),
    updated_by  VARCHAR(50),
    PRIMARY KEY (id),
    CONSTRAINT uk_job_position_code UNIQUE (code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE employee (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    employee_code    VARCHAR(20)  NOT NULL,
    last_name        VARCHAR(100) NOT NULL,
    first_name       VARCHAR(100) NOT NULL,
    register_no      VARCHAR(20),
    gender           VARCHAR(10),
    birth_date       DATE,
    email            VARCHAR(150),
    phone            VARCHAR(32),
    hire_date        DATE         NOT NULL,
    termination_date DATE,
    status           VARCHAR(20)  NOT NULL,
    branch_id        BIGINT       NOT NULL,
    department_id    BIGINT,
    position_id      BIGINT,
    manager_id       BIGINT,
    version          BIGINT       NOT NULL DEFAULT 0,
    created_at       DATETIME(6)  NOT NULL,
    created_by       VARCHAR(50),
    updated_at       DATETIME(6),
    updated_by       VARCHAR(50),
    PRIMARY KEY (id),
    CONSTRAINT uk_employee_code UNIQUE (employee_code),
    CONSTRAINT uk_employee_register_no UNIQUE (register_no),
    CONSTRAINT uk_employee_email UNIQUE (email),
    CONSTRAINT fk_employee_branch FOREIGN KEY (branch_id) REFERENCES branch (id),
    CONSTRAINT fk_employee_department FOREIGN KEY (department_id) REFERENCES department (id),
    CONSTRAINT fk_employee_position FOREIGN KEY (position_id) REFERENCES job_position (id),
    CONSTRAINT fk_employee_manager FOREIGN KEY (manager_id) REFERENCES employee (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE INDEX ix_employee_name ON employee (last_name, first_name);
CREATE INDEX ix_employee_status ON employee (status);

CREATE TABLE privilege (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    code        VARCHAR(50)  NOT NULL,
    name        VARCHAR(150) NOT NULL,
    module      VARCHAR(30)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_privilege_code UNIQUE (code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE role (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    code        VARCHAR(50)  NOT NULL,
    name        VARCHAR(150) NOT NULL,
    description VARCHAR(500),
    system_role BOOLEAN      NOT NULL DEFAULT FALSE,
    version     BIGINT       NOT NULL DEFAULT 0,
    created_at  DATETIME(6)  NOT NULL,
    created_by  VARCHAR(50),
    updated_at  DATETIME(6),
    updated_by  VARCHAR(50),
    PRIMARY KEY (id),
    CONSTRAINT uk_role_code UNIQUE (code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE role_privilege (
    role_id      BIGINT NOT NULL,
    privilege_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, privilege_id),
    CONSTRAINT fk_role_privilege_role FOREIGN KEY (role_id) REFERENCES role (id) ON DELETE CASCADE,
    CONSTRAINT fk_role_privilege_privilege FOREIGN KEY (privilege_id) REFERENCES privilege (id)
) ENGINE = InnoDB;

CREATE TABLE app_user (
    employee_id          BIGINT       NOT NULL,
    username             VARCHAR(50)  NOT NULL,
    password_hash        VARCHAR(100) NOT NULL,
    enabled              BOOLEAN      NOT NULL DEFAULT TRUE,
    locked               BOOLEAN      NOT NULL DEFAULT FALSE,
    failed_attempts      INT          NOT NULL DEFAULT 0,
    locked_until         DATETIME(6),
    last_login_at        DATETIME(6),
    password_changed_at  DATETIME(6),
    must_change_password BOOLEAN      NOT NULL DEFAULT TRUE,
    multi_branch_access  BOOLEAN      NOT NULL DEFAULT FALSE,
    pref_language        VARCHAR(5)   NOT NULL DEFAULT 'mn',
    pref_theme           VARCHAR(30),
    pref_date_format     VARCHAR(20),
    pref_font_size       VARCHAR(10),
    version              BIGINT       NOT NULL DEFAULT 0,
    created_at           DATETIME(6)  NOT NULL,
    created_by           VARCHAR(50),
    updated_at           DATETIME(6),
    updated_by           VARCHAR(50),
    PRIMARY KEY (employee_id),
    CONSTRAINT uk_app_user_username UNIQUE (username),
    CONSTRAINT fk_app_user_employee FOREIGN KEY (employee_id) REFERENCES employee (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE user_role (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES app_user (employee_id) ON DELETE CASCADE,
    CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES role (id)
) ENGINE = InnoDB;

CREATE TABLE refresh_token (
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    user_id      BIGINT      NOT NULL,
    token_hash   CHAR(64)    NOT NULL,
    expires_at   DATETIME(6) NOT NULL,
    revoked_at   DATETIME(6),
    created_at   DATETIME(6) NOT NULL,
    client_ip    VARCHAR(45),
    PRIMARY KEY (id),
    CONSTRAINT uk_refresh_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_token_user FOREIGN KEY (user_id) REFERENCES app_user (employee_id) ON DELETE CASCADE
) ENGINE = InnoDB;

CREATE INDEX ix_refresh_token_user ON refresh_token (user_id);

CREATE TABLE login_history (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    username       VARCHAR(50)  NOT NULL,
    user_id        BIGINT,
    success        BOOLEAN      NOT NULL,
    failure_reason VARCHAR(50),
    client_ip      VARCHAR(45),
    user_agent     VARCHAR(300),
    created_at     DATETIME(6)  NOT NULL,
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE INDEX ix_login_history_username ON login_history (username, created_at);
