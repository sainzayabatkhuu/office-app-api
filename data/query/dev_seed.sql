-- Development data for a fresh database (tables are created by Hibernate on first start).
-- Load with:  mysql --default-character-set=utf8mb4 -u root -p <db> < data/query/dev_seed.sql
-- Users (DEV ONLY, never on a shared server):
--   admin  / Admin123   ROLE_ADMIN, branch 000, multi-branch
--   viewer / Viewer123  ROLE_REPORT_VIEWER, branch 101; password already expired -> must change at first sign-in

INSERT INTO branch (sol_id, name, alternate_branch_code, branch_available) VALUES
  ('000', 'Төв салбар', 'HO', 1),
  ('101', 'Сүхбаатар салбар', 'B101', 1);

INSERT INTO privilege (description, value) VALUES
  ('View Roles', 'get:/roles'), ('Create Roles', 'post:/roles'), ('Update Roles', 'put:/roles'), ('Delete Roles', 'delete:/roles'),
  ('View Privileges', 'get:/privileges'), ('Create Privileges', 'post:/privileges'), ('Update Privileges', 'put:/privileges'), ('Delete Privileges', 'delete:/privileges'),
  ('View Users', 'get:/users'), ('Create Users', 'post:/users'), ('Update Users', 'put:/users'), ('Delete Users', 'delete:/users'),
  ('View Branches', 'get:/branches'), ('Create Branches', 'post:/branches'), ('Update Branches', 'put:/branches'), ('Delete Branches', 'delete:/branches'),
  ('View Customers', 'get:/customers'), ('Create Customers', 'post:/customers'), ('Update Customers', 'put:/customers'), ('Delete Customers', 'delete:/customers'),
  ('View Reports', 'get:/reports/*,get:/reports/report-list,get:/reports/details/*'), ('Create Reports', 'post:/reports'), ('Update Reports', 'put:/reports'), ('Delete Reports', 'delete:/reports'),
  ('View Report Permissions', 'get:/report-permissions'), ('Create Report Permissions', 'post:/report-permissions'), ('Update Report Permissions', 'put:/report-permissions'), ('Delete Report Permissions', 'delete:/report-permissions'),
  ('View Background History', 'get:/report-histories'), ('Generate Report', 'get:/reports/export'),
  ('View Core Parameters', 'get:/core-parameters'), ('Create Core Parameters', 'post:/core-parameters'), ('Update Core Parameters', 'put:/core-parameters'), ('Delete Core Parameters', 'delete:/core-parameters'),
  ('View Excel Exports', 'get:/export-monitor');

INSERT INTO role (name, description) VALUES ('ROLE_ADMIN', 'Бүх эрхтэй'), ('ROLE_REPORT_VIEWER', 'Тайлан харах, гаргах');
INSERT INTO roles_privileges (role_id, privilege_id) SELECT r.id, p.id FROM role r CROSS JOIN privilege p WHERE r.name = 'ROLE_ADMIN';
INSERT INTO roles_privileges (role_id, privilege_id) SELECT r.id, p.id FROM role r JOIN privilege p ON p.value LIKE 'get:/report%' WHERE r.name = 'ROLE_REPORT_VIEWER';

INSERT INTO users (username, email, password, enabled, deleted, account_non_locked, failed_attempt, multi_brnch_access, token_expired,
                   font_size, theme_name, user_language, firstname, lastname, last_password_change_date, branch_code,
                   modify_count, created_by, created_date) VALUES
  ('admin', 'admin@office.local', '$2a$10$2a1jmlEC/l9rJD1wD5KMjun.HTff9x0pTmgl8bPkRa1vu1s9Fu.BW', 1, 0, 1, 0, 1, 0,
   'font-s', 'cadet-green', 'mn', 'Систем', 'Админ', DATE_ADD(CURDATE(), INTERVAL 90 DAY), '000', 0, 'seed', NOW()),
  ('viewer', 'viewer@office.local', '$2a$10$3nHhl5DXHcuFSdQ0osst7OuNPXmB6BBtJls/cm3yRO/2TwPMeLTVi', 1, 0, 1, 0, 0, 0,
   'font-s', 'cadet-green', 'mn', 'Бат', 'Дорж', DATE_SUB(CURDATE(), INTERVAL 1 DAY), '101', 0, 'seed', NOW());

-- roles are assigned per branch through users_roles (branch_code is required)
INSERT INTO users_roles (branch_code, user_id, role_id)
SELECT u.branch_code, u.id, r.id FROM users u JOIN role r
  ON (u.username = 'admin' AND r.name = 'ROLE_ADMIN') OR (u.username = 'viewer' AND r.name = 'ROLE_REPORT_VIEWER');
