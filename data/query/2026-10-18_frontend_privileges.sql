-- Run once after deploying branch fix/frontend-api-hardening.
-- Every /api/v1 endpoint is now checked against the caller's privilege rules (method:path, relative to
-- /api/v1; a rule also covers sub-paths). Endpoints that had no privilege before need one now.
-- Table names are the ones Hibernate generates (privilege, role, roles_privileges); MySQL on Linux is case-sensitive.

-- 1. Excel export monitoring (CSDXLMNT / CSSXLMNT screens). Exporting itself (/export-monitor/export) is open
--    to every signed-in user.
INSERT INTO privilege (description, value)
SELECT 'View Excel Exports', 'get:/export-monitor'
WHERE NOT EXISTS (SELECT 1 FROM privilege WHERE value = 'get:/export-monitor');

-- 2. Report permission privileges were seeded with swapped descriptions; align them with their values.
UPDATE privilege SET description = 'View Report Permissions'   WHERE value = 'get:/report-permissions';
UPDATE privilege SET description = 'Create Report Permissions' WHERE value = 'post:/report-permissions';
UPDATE privilege SET description = 'Update Report Permissions' WHERE value = 'put:/report-permissions';
UPDATE privilege SET description = 'Delete Report Permissions' WHERE value = 'delete:/report-permissions';

-- 3. Administrators get every privilege, including the new one.
INSERT INTO roles_privileges (role_id, privilege_id)
SELECT r.id, p.id FROM role r CROSS JOIN privilege p
WHERE r.name = 'ROLE_ADMIN'
  AND NOT EXISTS (SELECT 1 FROM roles_privileges rp WHERE rp.role_id = r.id AND rp.privilege_id = p.id);

-- 4. users.failed_attempt is a primitive int in the entity; NULLs make loading the user fail.
UPDATE users SET failed_attempt = 0 WHERE failed_attempt IS NULL;

-- 5. Sign-in now enforces users.enabled (it was ignored before). Check nobody active is disabled by mistake:
--    SELECT username, enabled FROM users;
--    UPDATE users SET enabled = 1 WHERE username IN (...);
