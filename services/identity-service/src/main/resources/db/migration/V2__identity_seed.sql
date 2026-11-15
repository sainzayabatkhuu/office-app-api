-- Reference data: privileges, built-in roles and the bootstrap administrator.
-- Bootstrap login: admin / Admin@123  (must be changed on first login outside dev).

INSERT INTO privilege (code, name, module) VALUES
    ('USER_VIEW',          'View application users',              'IDENTITY'),
    ('USER_MANAGE',        'Create/modify users, reset passwords', 'IDENTITY'),
    ('EMPLOYEE_VIEW',      'View employees',                      'IDENTITY'),
    ('EMPLOYEE_MANAGE',    'Create/modify employees',             'IDENTITY'),
    ('ROLE_VIEW',          'View roles and privileges',           'IDENTITY'),
    ('ROLE_MANAGE',        'Create/modify roles',                 'IDENTITY'),
    ('ORG_MANAGE',         'Maintain branches, departments, positions', 'IDENTITY'),
    ('REPORT_VIEW',        'See reports available to my roles',   'REPORT'),
    ('REPORT_EXECUTE',     'Generate reports',                    'REPORT'),
    ('REPORT_MANAGE',      'Upload templates, manage report permissions', 'REPORT'),
    ('REPORT_HISTORY_ALL', 'See report history of all users',     'REPORT'),
    ('NOTIFICATION_SEND',  'Send notifications to other users',   'NOTIFICATION');

INSERT INTO role (code, name, description, system_role, created_at, created_by) VALUES
    ('ADMIN',        'System administrator', 'Full access',                                  TRUE,  NOW(6), 'system'),
    ('REPORT_ADMIN', 'Report administrator', 'Maintains report catalog and permissions',     TRUE,  NOW(6), 'system'),
    ('REPORT_USER',  'Report user',          'Generates reports permitted to their roles',   TRUE,  NOW(6), 'system'),
    ('HR_MANAGER',   'HR manager',           'Maintains employees and organization',         TRUE,  NOW(6), 'system');

INSERT INTO role_privilege (role_id, privilege_id)
SELECT r.id, p.id FROM role r CROSS JOIN privilege p WHERE r.code = 'ADMIN';

INSERT INTO role_privilege (role_id, privilege_id)
SELECT r.id, p.id FROM role r JOIN privilege p
    ON p.code IN ('REPORT_VIEW', 'REPORT_EXECUTE', 'REPORT_MANAGE', 'REPORT_HISTORY_ALL', 'ROLE_VIEW')
WHERE r.code = 'REPORT_ADMIN';

INSERT INTO role_privilege (role_id, privilege_id)
SELECT r.id, p.id FROM role r JOIN privilege p ON p.code IN ('REPORT_VIEW', 'REPORT_EXECUTE')
WHERE r.code = 'REPORT_USER';

INSERT INTO role_privilege (role_id, privilege_id)
SELECT r.id, p.id FROM role r JOIN privilege p
    ON p.code IN ('EMPLOYEE_VIEW', 'EMPLOYEE_MANAGE', 'USER_VIEW', 'ORG_MANAGE', 'REPORT_VIEW', 'REPORT_EXECUTE')
WHERE r.code = 'HR_MANAGER';

INSERT INTO branch (code, name, active, created_at, created_by) VALUES ('000', 'Head office', TRUE, NOW(6), 'system');
INSERT INTO department (code, name, branch_id, active, created_at, created_by)
SELECT 'IT', 'Information technology', id, TRUE, NOW(6), 'system' FROM branch WHERE code = '000';
INSERT INTO job_position (code, name, active, created_at, created_by) VALUES ('SYSADM', 'System administrator', TRUE, NOW(6), 'system');

INSERT INTO employee (employee_code, last_name, first_name, email, hire_date, status, branch_id, department_id, position_id, created_at, created_by)
SELECT 'E00001', 'Admin', 'System', 'admin@office.local', CURRENT_DATE, 'ACTIVE', b.id, d.id, p.id, NOW(6), 'system'
FROM branch b, department d, job_position p
WHERE b.code = '000' AND d.code = 'IT' AND p.code = 'SYSADM';

INSERT INTO app_user (employee_id, username, password_hash, enabled, must_change_password, multi_branch_access,
                      password_changed_at, created_at, created_by)
SELECT id, 'admin', '$2a$10$eE9dk273ddjlR0.6xmyZVuXM3jjutGQR7C9k6/Gbsw0qQXReLmKk.', TRUE, FALSE, TRUE, NOW(6), NOW(6), 'system'
FROM employee WHERE employee_code = 'E00001';

INSERT INTO user_role (user_id, role_id)
SELECT e.id, r.id FROM employee e, role r WHERE e.employee_code = 'E00001' AND r.code = 'ADMIN';
