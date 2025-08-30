DELIMITER $$

CREATE PROCEDURE sp_privilege_report()
BEGIN
    SELECT
        p.id,
        p.name,
        GROUP_CONCAT(r.name SEPARATOR ', ') AS assigned_roles
    FROM privilege p
             LEFT JOIN roles_privileges rp ON p.id = rp.privilege_id
             LEFT JOIN role r ON rp.role_id = r.id
    GROUP BY p.id, p.name
    ORDER BY p.name;
END$$

DELIMITER ;