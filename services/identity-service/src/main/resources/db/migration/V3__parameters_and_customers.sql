-- System parameters (CORPRM), customers (STDCIF) and the privileges for them and for the Excel export
-- monitor (CSDXLMNT/CSSXLMNT, enforced by report-service). Ported from office-app-api.

INSERT INTO privilege (code, name, module) VALUES
    ('PARAM_VIEW',      'View system parameters',               'CORE'),
    ('PARAM_MANAGE',    'Create/modify/delete system parameters', 'CORE'),
    ('CUSTOMER_VIEW',   'View customers',                       'CUSTOMER'),
    ('CUSTOMER_MANAGE', 'Create/modify/delete customers',       'CUSTOMER'),
    ('EXPORT_MONITOR',  'Monitor Excel exports of all users',   'REPORT');

-- V2 gave ADMIN every privilege that existed then; keep it a full-access role.
INSERT INTO role_privilege (role_id, privilege_id)
SELECT r.id, p.id FROM role r JOIN privilege p
    ON p.code IN ('PARAM_VIEW', 'PARAM_MANAGE', 'CUSTOMER_VIEW', 'CUSTOMER_MANAGE', 'EXPORT_MONITOR')
WHERE r.code = 'ADMIN';

CREATE TABLE core_parameter (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    param_type  VARCHAR(50)  NOT NULL,
    param_code  VARCHAR(50)  NOT NULL,
    param_name  VARCHAR(200) NOT NULL,
    version     BIGINT       NOT NULL DEFAULT 0,
    created_at  DATETIME(6)  NOT NULL,
    created_by  VARCHAR(50),
    updated_at  DATETIME(6),
    updated_by  VARCHAR(50),
    PRIMARY KEY (id),
    CONSTRAINT uk_core_parameter UNIQUE (param_type, param_code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- Customer (CIF). id = type letter + customer number (e.g. R000000001). Individual and corporate details
-- live in their own tables (JOINED inheritance). branch_code refers to branch.code.
CREATE TABLE customer (
    id                  VARCHAR(10)  NOT NULL,
    customer_type       VARCHAR(1)   NOT NULL,
    customer_number     VARCHAR(9)   NOT NULL,
    full_name           VARCHAR(200) NOT NULL,
    short_name          VARCHAR(50),
    branch_code         VARCHAR(10)  NOT NULL,
    customer_category   VARCHAR(30),
    is_private_customer VARCHAR(1),
    version             BIGINT       NOT NULL DEFAULT 0,
    created_at          DATETIME(6)  NOT NULL,
    created_by          VARCHAR(50),
    updated_at          DATETIME(6),
    updated_by          VARCHAR(50),
    PRIMARY KEY (id),
    CONSTRAINT uk_customer_number UNIQUE (customer_number),
    CONSTRAINT fk_customer_branch FOREIGN KEY (branch_code) REFERENCES branch (code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE INDEX ix_customer_branch ON customer (branch_code);

CREATE TABLE individual_customer (
    id                   VARCHAR(10)  NOT NULL,
    prefix1              VARCHAR(20),
    prefix2              VARCHAR(20),
    prefix3              VARCHAR(20),
    first_name           VARCHAR(100) NOT NULL,
    middle_name          VARCHAR(100),
    last_name            VARCHAR(100) NOT NULL,
    work_phone_isd       VARCHAR(5),
    work_phone           VARCHAR(32),
    home_phone_isd       VARCHAR(5),
    home_phone           VARCHAR(32),
    mobile_isd           VARCHAR(5),
    mobile               VARCHAR(32),
    fax_isd              VARCHAR(5),
    fax                  VARCHAR(32),
    email                VARCHAR(254),
    corr_address_name    VARCHAR(35),
    corr_address_code    VARCHAR(30),
    corr_line1           VARCHAR(35),
    corr_line2           VARCHAR(35),
    corr_line3           VARCHAR(35),
    corr_line4           VARCHAR(35),
    corr_pincode         VARCHAR(20),
    corr_country         VARCHAR(2),
    perm_address_name    VARCHAR(35),
    perm_address_code    VARCHAR(30),
    perm_line1           VARCHAR(35),
    perm_line2           VARCHAR(35),
    perm_line3           VARCHAR(35),
    perm_line4           VARCHAR(35),
    perm_pincode         VARCHAR(20),
    perm_country         VARCHAR(2),
    same_as_corr_addr    VARCHAR(1)   NOT NULL DEFAULT 'N',
    language             VARCHAR(5)   NOT NULL,
    communication_mode   VARCHAR(16),
    gender               VARCHAR(30),
    national_id          VARCHAR(50),
    birth_place          VARCHAR(200),
    birth_country        VARCHAR(2),
    date_of_birth        DATE,
    mothers_maiden_name  VARCHAR(200),
    resident_status      VARCHAR(20),
    passport_number      VARCHAR(50),
    passport_issue_date  DATE,
    passport_expiry_date DATE,
    kyc_status           VARCHAR(20),
    kyc_reference        VARCHAR(64),
    staff                BOOLEAN      NOT NULL DEFAULT FALSE,
    minor                BOOLEAN      NOT NULL DEFAULT FALSE,
    guardian             VARCHAR(200),
    PRIMARY KEY (id),
    CONSTRAINT fk_individual_customer FOREIGN KEY (id) REFERENCES customer (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE corporate_customer (
    id                   VARCHAR(10)   NOT NULL,
    name                 VARCHAR(140)  NOT NULL,
    telephone_isd        VARCHAR(5),
    telephone            VARCHAR(32),
    mobile_isd           VARCHAR(5),
    mobile               VARCHAR(32),
    fax_isd              VARCHAR(5),
    fax                  VARCHAR(32),
    email                VARCHAR(254),
    corr_address_name    VARCHAR(35),
    corr_address_code    VARCHAR(30),
    corr_line1           VARCHAR(35),
    corr_line2           VARCHAR(35),
    corr_line3           VARCHAR(35),
    corr_line4           VARCHAR(35),
    corr_pincode         VARCHAR(20),
    corr_country         VARCHAR(2),
    reg_address_name     VARCHAR(35),
    reg_address_code     VARCHAR(30),
    reg_line1            VARCHAR(35),
    reg_line2            VARCHAR(35),
    reg_line3            VARCHAR(35),
    reg_line4            VARCHAR(35),
    reg_pincode          VARCHAR(20),
    reg_country          VARCHAR(2),
    same_as_corr_addr    VARCHAR(1)    NOT NULL DEFAULT 'N',
    language             VARCHAR(5)    NOT NULL,
    communication_mode   VARCHAR(16),
    incorporation_date   DATE,
    capital              DECIMAL(20, 2),
    net_worth            DECIMAL(20, 2),
    currency             VARCHAR(3),
    kyc_status           VARCHAR(20),
    kyc_reference        VARCHAR(64),
    national_id          VARCHAR(50),
    ownership_type       VARCHAR(30),
    business_description VARCHAR(2000),
    PRIMARY KEY (id),
    CONSTRAINT fk_corporate_customer FOREIGN KEY (id) REFERENCES customer (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
