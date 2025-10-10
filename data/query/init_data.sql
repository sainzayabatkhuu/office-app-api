INSERT INTO roles (name, code, description) VALUES
('Admin', 'ROLE_ADMIN', 'Full access to all system functionalities; limited customers, branches'),
('Manager', 'ROLE_MANAGER', 'Can manage customers and reports, but limited system settings'),
('Senior', 'ROLE_SENIOR', 'Can manage customers and reports within their branch; limited system access.'),
('Teller', 'ROLE_TELLER', 'Can view customers and reports only'),
('Report Manager', 'ROLE_REPORT_MANAGER', 'Can manage and generate reports; limited system access.'),
('Report Viewer', 'ROLE_REPORT_VIEWER', 'Can view report histories, report lists, and report details only.'),
('Viewer', 'ROLE_VIEWER', 'Read-only access to all GET endpoints.');

INSERT INTO privilege (description, value) VALUES
  ('View Roles', 'get:/roles'),
  ('Create Roles', 'post:/roles'),
  ('Update Roles', 'put:/roles'),
  ('Delete Roles', 'delete:/roles'),

  ('View Privileges', 'get:/privileges'),
  ('Create Privileges', 'post:/privileges'),
  ('Update Privileges', 'put:/privileges'),
  ('Delete Privileges', 'delete:/privileges'),

  ('View Users', 'get:/users'),
  ('Create Users', 'post:/users'),
  ('Update Users', 'put:/users'),
  ('Delete Users', 'delete:/users'),

  ('View Branches', 'get:/branches'),
  ('Create Branches', 'post:/branches'),
  ('Update Branches', 'put:/branches'),
  ('Delete Branches', 'delete:/branches'),

  ('View Customers', 'get:/customers'),
  ('Create Customers', 'post:/customers'),
  ('Update Customers', 'put:/customers'),
  ('Delete Customers', 'delete:/customers'),

  ('View Reports', 'get:/reports/*,get:/reports/report-list,get:/reports/details/*'),
  ('Create Reports', 'post:/reports'),
  ('Update Reports', 'put:/reports'),
  ('Delete Reports', 'delete:/reports'),

  ('Create Report Permissions', 'get:/report-permissions'),
  ('Update Report Permissions', 'post:/report-permissions'),
  ('Delete Report Permissions', 'put:/report-permissions'),
  ('View Report Permissions', 'delete:/report-permissions'),

  ('View Background History', 'get:/report-histories'),
  ('Generate Report', 'get:/reports/export'),

  ('View Core Parameters', 'get:/core-parameters'),
  ('Create Core Parameters', 'post:/core-parameters'),
  ('Update Core Parameters', 'put:/core-parameters'),
  ('Delete Core Parameters', 'delete:/core-parameters');

-- admin
INSERT INTO roles_privileges (role_id, privilege_id)
SELECT 1, id FROM privilege;

-- ROLE_MANAGER
INSERT INTO roles_privileges (role_id, privilege_id) VALUES
(2, 13), -- View Customers
(2, 14), -- Create Customers
(2, 15), -- Update Customers
(2, 17), -- View Reports
(2, 18), -- Create Reports
(2, 19), -- Update Reports
(2, 21), -- Create Report Permissions
(2, 22), -- Update Report Permissions
(2, 27), -- View Core Parameters
(2, 31); -- Generate Report


-- ROLE_SENIOR
INSERT INTO roles_privileges (role_id, privilege_id) VALUES
(3, 13), -- View Customers
(3, 14), -- Create Customers
(3, 15), -- Update Customers
(3, 17), -- View Reports
(3, 18), -- Create Reports
(3, 19), -- Update Reports
(3, 31); -- Generate Report

-- ROLE_TELLER
INSERT INTO roles_privileges (role_id, privilege_id) VALUES
(4, 13), -- View Customers
(4, 17), -- View Reports
(4, 28); -- View Background History

-- ROLE_REPORT_MANAGER
INSERT INTO roles_privileges (role_id, privilege_id) VALUES
(5, 17), -- View Reports
(5, 18), -- Create Reports
(5, 19), -- Update Reports
(5, 21), -- Create Report Permissions
(5, 22), -- Update Report Permissions
(5, 31); -- Generate Report

-- ROLE_REPORT_VIEWER
INSERT INTO roles_privileges (role_id, privilege_id) VALUES
(6, 17), -- View Reports
(6, 24), -- View Report Permissions
(6, 28); -- View Background History

-- ROLE_VIEWER
INSERT INTO roles_privileges (role_id, privilege_id) VALUES
(7, 1),  -- View Roles
(7, 5),  -- View Privileges
(7, 9),  -- View Users
(7, 13), -- View Branches
(7, 17), -- View Reports
(7, 24), -- View Report Permissions
(7, 28), -- View Background History
(7, 27); -- View Core Parameters